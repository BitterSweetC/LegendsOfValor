package legends.service.impl;

import java.util.*;

import legends.entity.Combatant;
import legends.entity.Hero;
import legends.entity.ItemInstance;
import legends.entity.Monster;
import legends.entity.SessionStatistics;
import legends.entity.StatusEffect;
import legends.entity.item.FireSpell;
import legends.entity.item.IceSpell;
import legends.entity.item.LightningSpell;
import legends.entity.monster.Dragon;
import legends.entity.monster.Goblin;
import legends.entity.monster.Golem;
import legends.entity.monster.Spirit;
import legends.entity.monster.Vampire;
import legends.service.CombatService;
import legends.service.DamageCalculatorService;

public final class CombatServiceImpl implements CombatService {
    private final Random random;
    private final SessionStatistics statistics;
    private final DamageCalculatorService calculator = new DamageCalculatorServiceImpl();

    public CombatServiceImpl(Random random) {
        this(random, null);
    }

    public CombatServiceImpl(Random random, SessionStatistics statistics) {
        this.random = random;
        this.statistics = statistics;
    }

    @Override
    public List<String> attack(Hero hero, Monster target) {
        double rawDamage = calculator.getPhysical(hero);
        hero.getEquipment().wearWeapons();
        hero.commit(null);
        List<String> log = new ArrayList<>();
        hit(hero, target, rawDamage, true, log);
        return log;
    }

    @Override
    public List<String> cast(Hero hero, ItemInstance spell, Monster target) {
        if(spell.getLevel() > hero.getLevel())
            throw new IllegalArgumentException(spell.getName() + " requires hero level " + spell.getLevel()
                    + "; current level: " + hero.getLevel() + ".");

        String type = spell.getSpell().getType();
        String attribute;
        if(spell instanceof IceSpell)
            attribute = "damage";
        else if(spell instanceof FireSpell)
            attribute = "defense";
        else if(spell instanceof LightningSpell)
            attribute = "dodge chance";
        else
            throw new IllegalArgumentException("Unknown spell type: " + type);

        long mp = (long) Math.ceil(spell.getManaCost());
        double rawDamage = calculator.getSpell(hero, spell);
        hero.spendMp(mp);
        spell.consume();
        hero.getInventory().remove(spell);
        hero.commit(null);

        List<String> log = new ArrayList<>();
        log.add(hero.getName() + " casts " + spell.getName() + " on " + target.getName() + " (" + mp + " MP)");
        if(hit(hero, target, rawDamage, false, log) && target.isAlive()) {
            target.effect("spell_" + type, type, 0.10, 3);
            log.add(target.getName() + ": " + type + " reduces " + attribute
                    + " by 10% for 3 rounds including this round (reapplying refreshes duration; does not stack)");
        }
        return log;
    }

    @Override
    public List<String> monsterAttack(Monster monster, Hero target) {
        List<String> log = new ArrayList<>();
        hit(monster, target, monster.getDamage(), true, log);
        return log;
    }

    @Override
    public List<String> potion(Hero hero, ItemInstance item, int rounds) {
        String kind = item.getPotion().getEffectType();
        boolean resource = kind.equals("HEALTH") || kind.equals("MANA");
        double before = kind.equals("HEALTH") ? hero.getHp() : kind.equals("MANA") ? hero.getMp() : hero.getStat(kind);
        hero.drink(item, rounds);
        double after = kind.equals("HEALTH") ? hero.getHp() : kind.equals("MANA") ? hero.getMp() : hero.getStat(kind);
        hero.commit(null);
        String attribute = kind.equals("HEALTH") ? "HP" : kind.equals("MANA") ? "MP" : kind;
        String duration = rounds == 0 ? "until battle ends" : "for " + rounds + " rounds";
        String result = String.format(resource ? "%s +%.0f (%.0f -> %.0f)" : "%s +%.2f (%.2f -> %.2f) " + duration,
                attribute, after - before, before, after);
        return Collections.singletonList(hero.getName() + " used " + item.getName() + ": " + result);
    }

    @Override
    public List<String> equip(Hero hero, ItemInstance item, String grip) {
        hero.getEquipment().equip(hero, item, grip);
        hero.commit(null);
        return Collections.singletonList(hero.getName() + " equipped " + item.getName() + " (" + grip + ")");
    }

    @Override
    public List<String> unequip(Hero hero, ItemInstance item) {
        hero.getEquipment().unequip(item);
        hero.commit(null);
        return Collections.singletonList(hero.getName() + " unequipped " + item.getName());
    }

    @Override
    public boolean hit(Combatant source, Combatant target, double rawDamage, boolean physical, List<String> log) {
        String side = source instanceof Hero ? "Hero attack: " : "Enemy attack: ";
        double dodgeBonus = 0;
        if(target instanceof Spirit && random.nextDouble() < 0.15)
            dodgeBonus = 0.10;

        if(random.nextDouble() < calculator.getDodge(target, dodgeBonus)) {
            log.add(side + source.getName() + " -> " + target.getName() + ": dodged");
            return false;
        }

        boolean critical = source instanceof Hero && random.nextDouble() < calculator.getCritical((Hero) source);
        if(critical)
            rawDamage *= 1.5;
        if(source instanceof Monster)
            rawDamage = applyAttackPassive((Monster) source, rawDamage);

        double reductionMultiplier = damageReduction(target, physical);
        long beforeHp = target.getHp();
        long actualDamage = target.hurt(calculator.getDamage(rawDamage, target.getDefense(), reductionMultiplier));
        if(statistics != null)
            statistics.damage(source, target, actualDamage);
        log.add(side + source.getName() + " -> " + target.getName() + ": " + actualDamage + " damage"
                + (critical ? " (critical)" : "") + " | HP " + beforeHp + " -> " + target.getHp() + "/" + target.getMaxHp()
                + (!target.isAlive() ? (target instanceof Hero ? "; fainted" : "; defeated") : ""));

        applyHitEffects(source, target, actualDamage, log);
        return true;
    }

    private double applyAttackPassive(Monster monster, double rawDamage) {
        if(monster instanceof Dragon && random.nextDouble() < 0.20)
            rawDamage *= 1.30;
        if(monster instanceof Goblin && random.nextDouble() < 0.18)
            rawDamage *= 1.50;
        return rawDamage;
    }

    private double damageReduction(Combatant target, boolean physical) {
        double multiplier = 1;
        for(StatusEffect effect : target.getEffects())
            if(effect.getKind().equals("REDUCTION"))
                multiplier *= 1 - effect.getValue();
        if(physical && target instanceof Golem)
            multiplier *= 1 - 0.15;
        return multiplier;
    }

    private void applyHitEffects(Combatant source, Combatant target, long actualDamage, List<String> log) {
        if(target instanceof Hero) {
            ItemInstance armor = ((Hero) target).getEquipment().getArmor();
            if(armor != null && actualDamage > 0)
                armor.wear();
        }
        if(source instanceof Vampire && random.nextDouble() < 0.25) {
            long beforeHp = source.getHp();
            source.heal((long) Math.ceil(actualDamage * 0.35));
            log.add("Life steal: " + source.getName() + " | HP +" + (source.getHp() - beforeHp)
                    + " (" + beforeHp + " -> " + source.getHp() + "/" + source.getMaxHp() + ")");
        }
    }
}