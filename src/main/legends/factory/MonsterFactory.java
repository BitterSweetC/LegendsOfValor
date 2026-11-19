package legends.factory;

import legends.entity.Monster;
import legends.entity.monster.Dragon;
import legends.entity.monster.Exoskeleton;
import legends.entity.monster.Goblin;
import legends.entity.monster.Golem;
import legends.entity.monster.Spirit;
import legends.entity.monster.Vampire;
import legends.entity.template.MonsterTto;

// Turns a monster row from monsters.txt into the matching Monster subclass.
public final class MonsterFactory {
    private MonsterFactory() {
    }

    public static Monster create(MonsterTto definition, int level) {
        switch(definition.getType()) {
            case "DRAGON" :
                return new Dragon(definition, level);
            case "EXOSKELETON" :
                return new Exoskeleton(definition, level);
            case "SPIRIT" :
                return new Spirit(definition, level);
            case "GOBLIN" :
                return new Goblin(definition, level);
            case "GOLEM" :
                return new Golem(definition, level);
            case "VAMPIRE" :
                return new Vampire(definition, level);
            default :
                throw new IllegalArgumentException("Unknown monster type: " + definition.getType());
        }
    }
}
