package legends.entity.item;

import legends.entity.template.SpellTto;

// Fire spell: lowers the target's defense for 3 rounds.
public class FireSpell extends Spell {
    public FireSpell(long id, SpellTto definition, int level, boolean starter) {
        super(id, definition, level, starter);
    }
}
