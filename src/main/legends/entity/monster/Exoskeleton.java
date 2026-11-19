package legends.entity.monster;

import legends.entity.Monster;
import legends.entity.template.MonsterTto;

//Exoskeleton can get 30% more defense when its HP drops below 40%
public class Exoskeleton extends Monster {
    public Exoskeleton(MonsterTto definition, int level) {
        super(definition, level);
    }
}
