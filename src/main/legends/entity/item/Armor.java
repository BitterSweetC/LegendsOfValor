package legends.entity.item;

import legends.entity.ItemInstance;
import legends.entity.template.ArmorTto;

// Body armor that adds defense.
public class Armor extends ItemInstance {
    public Armor(long id, ArmorTto definition, int level, boolean starter) {
        super(id, definition, level, starter);
    }
}
