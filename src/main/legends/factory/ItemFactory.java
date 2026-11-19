package legends.factory;

import legends.entity.ItemInstance;
import legends.entity.item.Armor;
import legends.entity.item.FireSpell;
import legends.entity.item.IceSpell;
import legends.entity.item.LightningSpell;
import legends.entity.item.Potion;
import legends.entity.item.Weapon;
import legends.entity.template.ArmorTto;
import legends.entity.template.ItemTto;
import legends.entity.template.PotionTto;
import legends.entity.template.SpellTto;
import legends.entity.template.WeaponTto;

/** Turns an item row from the data files into the matching ItemInstance subclass. */
public final class ItemFactory {
    private ItemFactory() {
    }

    public static ItemInstance create(long id, ItemTto definition, int level, boolean starter) {
        if(definition instanceof WeaponTto)
            return new Weapon(id, (WeaponTto) definition, level, starter);
        if(definition instanceof ArmorTto)
            return new Armor(id, (ArmorTto) definition, level, starter);
        if(definition instanceof PotionTto)
            return new Potion(id, (PotionTto) definition, level, starter);
        if(definition instanceof SpellTto) {
            SpellTto spell = (SpellTto) definition;
            switch(spell.getType()) {
                case "FIRE" :
                    return new FireSpell(id, spell, level, starter);
                case "ICE" :
                    return new IceSpell(id, spell, level, starter);
                case "LIGHTNING" :
                    return new LightningSpell(id, spell, level, starter);
                default :
                    throw new IllegalArgumentException("Unknown spell type: " + spell.getType());
            }
        }
        throw new IllegalArgumentException("Unknown item: " + definition.getId());
    }
}
