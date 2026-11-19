package legends.factory;

import legends.config.GameConfig;
import legends.entity.Hero;
import legends.entity.hero.Paladin;
import legends.entity.hero.Sorcerer;
import legends.entity.hero.Warrior;
import legends.entity.template.HeroTto;

/** Turns a hero row from heroes.txt into the matching Hero subclass. */
public final class HeroFactory {
    private HeroFactory() {
    }

    public static Hero create(HeroTto definition, GameConfig config) {
        switch(definition.getHeroClass()) {
            case "WARRIOR" :
                return new Warrior(definition, config);
            case "SORCERER" :
                return new Sorcerer(definition, config);
            case "PALADIN" :
                return new Paladin(definition, config);
            default :
                throw new IllegalArgumentException("Unknown hero class: " + definition.getHeroClass());
        }
    }
}
