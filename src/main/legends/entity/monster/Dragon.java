package legends.entity.monster;

import legends.entity.Monster;
import legends.entity.template.MonsterTto;

//Dragon have 20% chance for an attack 30% more damage
public class Dragon extends Monster {
    public Dragon(MonsterTto definition, int level) {
        super(definition, level);
    }
}
