package legends.entity.item;

import legends.entity.template.SpellTto;

// Lightning spell: lowers the target's dodge chance for 3 rounds. The effect itself is handled in BattleServiceImpl
public class LightningSpell extends Spell {
    public LightningSpell(long id, SpellTto definition, int level, boolean starter) {
        super(id, definition, level, starter);
    }
}
