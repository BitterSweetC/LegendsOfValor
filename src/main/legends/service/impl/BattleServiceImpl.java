package legends.service.impl;

import java.util.*;

import legends.entity.Battle;
import legends.entity.Combatant;
import legends.entity.Hero;
import legends.entity.ItemInstance;
import legends.entity.Monster;
import legends.entity.Skill;
import legends.enums.SkillEffectType;
import legends.enums.SkillTargetMode;
import legends.service.BattleService;
import legends.service.CombatService;
import legends.service.DamageCalculatorService;

// Turn-based Battle: Hero Actions, Enemy Actions, Recovery, Victory.
// Single-action effects are in CombatService, shared with Legends of Valor.
public final class BattleServiceImpl implements BattleService {
    private final Random random;
    private final DamageCalculatorService calculator;

    public BattleServiceImpl(Random random) {
        this.random = random;
        calculator = new DamageCalculatorServiceImpl();
    }

    private CombatService combat(Battle battle) {
        return new CombatServiceImpl(random, battle.getStatistics());
    }

    @Override
    public List<String> attack(Battle battle, Hero hero, Monster monster) {
        List<String> log = combat(battle).attack(hero, monster);
        battle.acted();
        battle.getStatistics().action(battle, "Attack", log);
        return log;
    }

    @Override
    public List<String> cast(Battle battle, Hero hero, ItemInstance spell, Monster monster) {
        List<String> log = combat(battle).cast(hero, spell, monster);
        battle.acted();
        battle.getStatistics().action(battle, "Spell", log);
        return log;
    }

    @Override
    public List<String> potion(Battle battle, Hero hero, ItemInstance item) {
        long beforeHp = hero.getHp(), beforeMp = hero.getMp();
        List<String> log = combat(battle).potion(hero, item, 0);   // 0 = until battle ends
        battle.getStatistics().potion(hero, beforeHp, beforeMp);
        battle.acted();
        battle.getStatistics().action(battle, "Potion", log);
        return log;
    }

    @Override
    public List<String> equip(Battle battle, Hero hero, ItemInstance item, String grip) {
        List<String> log = combat(battle).equip(hero, item, grip);
        battle.acted();
        battle.getStatistics().action(battle, "Equipment", log);
        return log;
    }

    @Override
    public List<String> unequip(Battle battle, Hero hero, ItemInstance item) {
        List<String> log = combat(battle).unequip(hero, item);
        battle.acted();
        battle.getStatistics().action(battle, "Equipment", log);
        return log;
    }

    @Override
    public List<String> skill(Battle battle, Hero hero, Skill skill, List<Combatant> targets) {
        if(!hero.isReady(skill))
            throw new IllegalArgumentException(skill.getName() + " is cooling down. Choose another action; viewing or canceling menus does not advance cooldown.");

        CombatService combat = combat(battle);
        List<Combatant> expected = skillTargets(battle, hero, skill, targets);
        SkillEffectType kind = skill.getEffectType();
        double multiplier = skill.getMultiplier();
        int duration = skill.getDuration();
        List<String> log = new ArrayList<>();
        boolean attack = kind == SkillEffectType.PHYSICAL || kind == SkillEffectType.MAGIC;
        boolean physical = kind == SkillEffectType.PHYSICAL;
        double rawDamage = attack ? (physical ? calculator.getPhysical(hero) : calculator.getMagic(hero)) * multiplier : 0;
        hero.commit(skill);
        if(physical)
            hero.getEquipment().wearWeapons();
        log.add(hero.getName() + " uses " + skill.getName());

        for(Combatant target : expected) {
            if(!target.isAlive())
                continue;
            if(attack)
                combat.hit(hero, target, rawDamage, physical, log);
            else {
                target.effect(skill.getId(), kind.name(), multiplier, duration);
                battle.getStatistics().record(battle, "Skill effect",
                        Collections.singletonList(hero.getName() + " -> " + target.getName() + ": "
                                + skill.getName() + " " + kind + " " + multiplier
                                + " for " + duration + " rounds"));
            }
            if(battle.isOver())
                break;
        }

        battle.acted();
        battle.getStatistics().action(battle, "Skill", log);
        return log;
    }

    private List<Combatant> skillTargets(Battle battle, Hero hero, Skill skill, List<Combatant> targets) {
        SkillTargetMode mode = skill.getTargetMode();
        List<Combatant> expected = new ArrayList<>();

        if(mode == SkillTargetMode.SELF)
            expected.add(hero);
        else if(mode == SkillTargetMode.ALL_ENEMIES)
            expected.addAll(battle.getLiving());
        else
            expected.addAll(targets);
        return expected;
    }

    @Override
    public List<String> finishRound(Battle battle) {
        List<String> log = new ArrayList<>();
        if(battle.isOver())
            return log;

        CombatService combat = combat(battle);
        // monsters pick random living hero as target
        for(Monster monster : battle.getLiving()) {
            List<Hero> living = battle.getParty().getLiving();
            if(living.isEmpty())
                break;
            Hero hero = living.get(random.nextInt(living.size()));
            log.addAll(combat.monsterAttack(monster, hero));
            if(battle.isOver())
                break;
        }

        battle.getStatistics().record(battle, "Monster attack", log);
        if(battle.isOver())
            return log;

        List<Combatant> all = new ArrayList<>(battle.getParty().getHeroes());
        all.addAll(battle.getMonsters());

        List<String> recovery = new ArrayList<>();
        for(Hero hero : battle.getParty().getLiving()) {
            long beforeHp = hero.getHp(), beforeMp = hero.getMp();
            hero.heal(hero.getHpRecovery());
            hero.restoreMp(hero.getMpRecovery());
            recovery.add("Round recovery: " + hero.getName() + " | HP +" + (hero.getHp() - beforeHp)
                    + " (" + beforeHp + " -> " + hero.getHp() + "/" + hero.getMaxHp() + ") | MP +"
                    + (hero.getMp() - beforeMp) + " (" + beforeMp + " -> " + hero.getMp() + "/" + hero.getMaxMp() + ")");
        }
        battle.getStatistics().record(battle, "Round recovery", recovery);
        log.addAll(recovery);

        for(Combatant target : all)
            target.endEffects();
        battle.nextRound();
        return log;
    }

    @Override
    public boolean settle(Battle battle) {
        if(!battle.isOver() || battle.isSettled())
            return false;
        battle.markSettled();
        battle.getStatistics().settled(battle);
        if(!battle.isVictory())
            return false;

        List<Hero> survivors = battle.getParty().getLiving();
        long experienceReward = 0, gold = 0;

        for(Monster monster : battle.getMonsters()) {
            experienceReward += monster.getExp();
            gold += monster.getGold();
        }

        for(Hero hero : battle.getParty().getHeroes())
            hero.endBattle();

        for(Hero hero : survivors) {
            hero.credit(gold);
            hero.gainExperience(experienceReward);
            battle.getStatistics().reward(hero, gold, experienceReward);
        }

        for(Hero hero : battle.getParty().getHeroes())
            if(!survivors.contains(hero))
                hero.revive();
        return true;
    }
}