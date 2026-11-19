package legends.entity.item;

import legends.entity.ItemInstance;
import legends.entity.template.ItemTto;

// A single-use item. It is removed from the inventory after use
public abstract class Consumable extends ItemInstance {
    protected Consumable(long id, ItemTto definition, int level, boolean starter) {
        super(id, definition, level, starter);
    }
}
