package legends.entity.monster;

import legends.entity.Monster;
import legends.entity.template.MonsterTto;

/** Golem: takes 15% less physical damage. The ability itself is handled in BattleServiceImpl. */
public class Golem extends Monster {
    public Golem(MonsterTto definition, int level) {
        super(definition, level);
    }
}
