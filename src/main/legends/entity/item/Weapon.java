package legends.entity.item;

import legends.entity.ItemInstance;
import legends.entity.template.WeaponTto;

// A weapon held in one or both hands
public class Weapon extends ItemInstance {
    public Weapon(long id, WeaponTto definition, int level, boolean starter) {
        super(id, definition, level, starter);
    }
}
