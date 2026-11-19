package legends.entity.item;

import legends.entity.template.SpellTto;

/**
 * A single-use market spell that damages and weakens a monster.
 * Abstract: every spell is a FireSpell, IceSpell or LightningSpell.
 */
public abstract class Spell extends Consumable {
    protected Spell(long id, SpellTto definition, int level, boolean starter) {
        super(id, definition, level, starter);
    }
}
