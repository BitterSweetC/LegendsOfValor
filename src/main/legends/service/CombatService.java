package legends.service;

import java.util.List;

import legends.entity.Combatant;
import legends.entity.Hero;
import legends.entity.ItemInstance;
import legends.entity.Monster;

/** Single-action combat effects. Never advances turns or rounds. */
public interface CombatService {
    List<String> attack(Hero hero, Monster target);
    List<String> cast(Hero hero, ItemInstance spell, Monster target);
    List<String> monsterAttack(Monster monster, Hero target);
    List<String> potion(Hero hero, ItemInstance item, int rounds);
    List<String> equip(Hero hero, ItemInstance item, String grip);
    List<String> unequip(Hero hero, ItemInstance item);

    // Dodge, critical, passives, reductions, armor wear, life steal.
    // Returns false if attack was dodged
    boolean hit(Combatant source, Combatant target, double rawDamage, boolean physical, List<String> log);
}