package legends.entity.monster;

import legends.entity.Monster;
import legends.entity.template.MonsterTto;

//Vampire 25% chance to heal 35% of the damage it deals
public class Vampire extends Monster {
    public Vampire(MonsterTto definition, int level) {
        super(definition, level);
    }
}
