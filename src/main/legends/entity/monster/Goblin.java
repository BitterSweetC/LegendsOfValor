package legends.entity.monster;

import legends.entity.Monster;
import legends.entity.template.MonsterTto;

// Goblin 18% chance for an attack 50% more damage
public class Goblin extends Monster {
    public Goblin(MonsterTto definition, int level) {
        super(definition, level);
    }
}
