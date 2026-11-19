package legends.entity.item;

import legends.entity.template.PotionTto;

// A single-use potion that restores HP/MP or boosts an attribute
public class Potion extends Consumable {
    public Potion(long id, PotionTto definition, int level, boolean starter) {
        super(id, definition, level, starter);
    }
}
