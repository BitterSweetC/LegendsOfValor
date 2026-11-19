package legends.entity.item;

import legends.entity.template.SpellTto;

// Ice spell: lowers the target's damage for 3 rounds. The effect itself is handled in BattleServiceImpl.
public class IceSpell extends Spell {
    public IceSpell(long id, SpellTto definition, int level, boolean starter) {
        super(id, definition, level, starter);
    }
}
