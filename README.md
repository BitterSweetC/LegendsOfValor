# CS611 - Assignment
## Legends: Monsters and Heroes & Legends of Valor
---------------------------------------------------------------------------
This is a Java program that runs in the terminal and has two games. When it starts, you can choose which game to play. Both games use the same heroes, monsters, items, and markets. There are three hero classes: Warrior, Sorcerer, and Paladin.

- **Legends: Monsters and Heroes:** You can choose 1 to 3 heroes and move around a 10x10 map. You can fight monsters, buy and sell items, change equipment, and get experience and gold.
- **Legends of Valor:** It is like a small MOBA game. You choose three heroes and put each one in a lane of an 8x8 board. The heroes start in their Nexus at the bottom, and the monsters start in their Nexus at the top. The heroes win if one of them reaches the monsters' Nexus, and the monsters win if one of them reaches the heroes' Nexus. On the way, heroes can fight monsters, cast spells, use potions, change equipment, teleport, recall, and buy and sell items.

## Files
---------------------------------------------------------------------------

### Core & Controller

- `src/main/legends/controller/LvGameController.java`: Runs Legends of Valor. It lets the player choose three heroes and their lanes, runs the heroes' turn and the monsters' turn, handles the market, the rewards, the respawns, and the new waves, and checks who wins.
- `src/main/legends/Main.java`: Reads the command-line options, loads the shared game data once, and displays the game selector. Choose `1` for Monsters and Heroes, `2` for Legends of Valor, or `0` to exit.
- `src/main/legends/controller/GameController.java`: The parent class of the two game controllers. It keeps the input, the game data, the seed, and the color option, and it has the shared helpers for reading input, choosing from a list, and printing colored messages.
- `src/main/legends/controller/MhGameController.java`: Runs Monsters and Heroes. It reads the player's input and shows the menus. It switches between choosing heroes, exploring, fighting, shopping, and game over.

### Configuration & Data Loading

- `src/main/legends/config/ConfigLoader.java`: Reads the text files in `data/`. The columns are separated by `|`. It uses the values to create hero, monster, item, skill, and equipment rule data.
- `src/main/legends/config/GameConfig.java`: Stores the data loaded from the files. Other classes can read its lists but cannot add or remove entries from them.

### Data Files (`data/`)

- `heroes.txt`: Stores starting stats, HP and MP growth, starting gold, and experience for each hero.
- `monsters.txt`: Stores monster base stats, stat growth per level, spawn weights, and exp/gold rewards.
- `weapons.txt`: Stores weapon prices, physical damage, hand requirements (1 or 2 hands), and magic bonuses.
- `armor.txt`: Stores armor prices, defense values, and armor types (`LIGHT`, `MEDIUM`, `HEAVY`).
- `potions.txt`: Stores potion prices, recovery amounts, and attribute boost limits.
- `spells.txt`: Stores single-use market spells, including level requirements, MP costs, damage, and spell types.
- `skills.txt`: Stores unlockable class skills, MP costs, cooldowns, target modes, and effects.
- `equipment-rules.txt`: Stores weapon and armor restrictions for each hero class, dual-wield rules, and starting weapons.

### Entities & Data Models

- `src/main/legends/entity/ValorWorld.java`: Stores the 8x8 Legends of Valor board and the position of every hero and monster. It checks all the moving rules, finds the monsters or heroes in range, handles teleport, recall, respawn, and removing obstacles, gives the terrain bonus, checks if someone reaches a Nexus, and prints the board.
- `src/main/legends/entity/Position.java`: Stores one row and column on the board. It cannot be changed after it is created.
- `src/main/legends/entity/LvGameSession.java`: Stores the board, the three hero slots, the round number, and whether the game is over. It decides which hero acts next and which heroes are waiting to respawn.
- `src/main/legends/entity/HeroSlot.java`: Stores one hero with its label (H1 to H3), its lane, its home Nexus space, whether it has acted this round, and whether it is waiting to respawn.
- `src/main/legends/entity/hero/`: `Warrior.java`, `Sorcerer.java`, and `Paladin.java` are the three hero classes. They inherit from Hero.
- `src/main/legends/entity/monster/`: `Dragon.java`, `Exoskeleton.java`, `Spirit.java`, `Goblin.java`, `Golem.java`, and `Vampire.java` are the monster types. They inherit from Monster and each one has its own ability.
- `src/main/legends/entity/item/`: `Weapon.java` and `Armor.java` inherit from ItemInstance. `Consumable.java` is the abstract parent of `Potion.java` and `Spell.java`, and `Spell.java` is the abstract parent of `FireSpell.java`, `IceSpell.java`, and `LightningSpell.java`.
- `src/main/legends/entity/Combatant.java`: The parent class of Hero and Monster. It stores their HP and level, and handles damage, healing, and status effects.
- `src/main/legends/entity/Hero.java`: Stores a hero's MP, properties, inventory, equipment, skills, money, and experience. It also handles potions, skill cooldowns, and leveling up.
- `src/main/legends/entity/Monster.java`: Stores a monster's type and uses its level to calculate its properties and rewards. Different monster types have different abilities.
- `src/main/legends/entity/Party.java`: Stores the selected heroes and their position on the map. It can find the living heroes and the team's highest level.
- `src/main/legends/entity/MhWorld.java`: Stores the Monsters and Heroes map and its markets. It checks whether the team can move to a place and prints the map.
- `src/main/legends/entity/Battle.java`: Stores the monsters and the current battle round. It decides which hero acts next and records whether the battle rewards have been given.
- `src/main/legends/entity/Market.java`: Stores the items in one market. It also remembers which items were sold by players.
- `src/main/legends/entity/Inventory.java`: Stores the items owned by a hero.
- `src/main/legends/entity/ItemInstance.java`: Stores one item's ID, level, price, durability, and whether it has been used. Items with the same name can have different values.
- `src/main/legends/entity/Equipment.java`: Stores the hero's equipped weapons and armor. It checks class restrictions and hand choices, and handles broken weapons.
- `src/main/legends/entity/Skill.java`: Stores a skill's name, class, unlock level, MP cost, cooldown, target, and effect.
- `src/main/legends/entity/StatusEffect.java`: Stores an effect's type, strength, and remaining time.
- `src/main/legends/entity/MhGameSession.java`: Stores the Monsters and Heroes team, world, current battle, random number generator, market service, statistics, and number of victories.
- `src/main/legends/entity/SessionStatistics.java`: Counts successful actions, actual damage, potion recovery, trades, and hero growth. It also records the actions and saves a text report.

### Entity Templates (`entity/template/`)

- `ItemTto.java`: The parent class for item templates. It stores the item ID, name, and base price.
- `HeroTto.java`: Stores a hero's starting properties, HP and MP growth, starting gold, and starting experience.
- `MonsterTto.java`: Stores a monster's starting properties, growth, selection weight, and rewards per level.
- `WeaponTto.java`, `ArmorTto.java`, `PotionTto.java`, `SpellTto.java`: Store the shared properties of weapons, armor, potions, and spells.
- `EquipmentRuleTto.java`: Stores which weapons and armor each hero class can use. It also stores whether the class can dual wield and which weapon it starts with.

### Services & Business Logic (`service/` and `service/impl/`)

- `CombatService.java` & `CombatServiceImpl.java`: Handle attacks, spells, potions, equipping, and monster attacks in Legends of Valor.
- `BattleService.java` & `BattleServiceImpl.java`: Handle hero actions, monster attacks, round recovery, and battle rewards.
- `DamageCalculatorService.java` & `DamageCalculatorServiceImpl.java`: Calculate attack power, spell damage, damage after defense, critical chances, and dodge chances.
- `MarketService.java` & `MarketServiceImpl.java`: Create market items, calculate prices, and handle buying, selling, repairing, and restocking.
- `ExplorationService.java` & `ExplorationServiceImpl.java`: Decide what happens when the team enters a common tile. The team may meet monsters, find treasure, or find nothing.

### Factories, Enums & Utilities

- `src/main/legends/factory/VWorldGenerator.java`: Creates a random Legends of Valor board with the walls, the two Nexus rows, and the different spaces in the lanes.
- `src/main/legends/factory/MonsterWaveGenerator.java`: Creates one monster for each lane. The monsters have the same level as the highest level hero.
- `src/main/legends/factory/HeroFactory.java`, `MonsterFactory.java`, `ItemFactory.java`: Create the right hero, monster, or item class from the data.
- `src/main/legends/factory/MhWorldGenerator.java`: Creates the Monsters and Heroes map and places the markets and blocked tiles. It uses BFS to check that all walkable places are connected.
- `src/main/legends/factory/MhEncounterGenerator.java`: Creates the Monsters and Heroes monsters based on the team's size and highest level. It uses weights to choose their number, levels, and names.
- `src/main/legends/utils/RandomWeights.java`: Chooses a random result from a list of weights. A larger weight gives a higher chance.
- `src/main/legends/enums/`: `GameState.java`, `TileType.java`, `SkillEffectType.java`, and `SkillTargetMode.java` list the possible game states, tile types, skill effects, and skill targets.

## Notes
---------------------------------------------------------------------------

1. The board is 8x8. Columns 2 and 5 are walls, so there are three lanes and each lane is two columns wide. Row 0 is the monsters' Nexus and row 7 is the heroes' Nexus. The other 36 spaces are shuffled from 8 Plain, 8 Bush, 8 Cave, 8 Koulou, and 4 Obstacles, so every type of space is on the board.
2. A hero standing on Bush gets 10% more DEX, on Cave 10% more AGI, and on Koulou 10% more STR. The bonus goes away when the hero leaves the space and it does not stack. Monsters do not get terrain bonuses.
3. You choose three heroes and a lane for each one. The hero starts in the left Nexus space of its lane, and this space is its home. Monsters start in the right Nexus space of each lane.
4. Every round the heroes act first and then the monsters act. Every living hero can do one action. The market (`B`), help (`H`), hero info (`I`), showing the board (`P`), a cancel (`0`), and a failed action do not use the turn.
5. Heroes move one space up, down, left, or right. They cannot move into a wall, an obstacle, a space with another hero, or past a monster in their lane. A hero can attack a monster in its own space or in the 8 spaces around it.
6. `T` moves a hero to the left, right, or behind a hero in another lane. `R` sends a hero back to its home space. `O` turns an obstacle next to the hero into a Plain space.
7. A hero can only use the market while standing in the heroes' Nexus. Buying, selling, and repairing do not use the turn.
8. In the monsters' turn, a monster attacks a hero in range. If no hero is in range, it moves one space toward the heroes' Nexus and stays in its own lane. A monster cannot move past a hero.
9. When a monster is killed, every living hero gets its gold and experience. Living heroes get back 10% of their HP and MP every round. A dead hero comes back at its home space in the next round with full HP and MP.
10. Every 8 rounds a new wave of three monsters comes, one in each lane, with the same level as the highest level hero. The game ends right away when a hero reaches the monsters' Nexus or a monster reaches the heroes' Nexus.

## Challenges
---------------------------------------------------------------------------

1. In Monsters and Heroes the whole team moves together so there is only one position. In Legends of Valor every hero and every monster moves on its own. At first we just used row and column numbers, but too many methods needed a position, so we made the `Position` class and keep all the positions only in `ValorWorld`. So there is never a different copy of a position somewhere else.
2. There are many moving rules: walls, obstacles, two heroes in one space, two monsters in one space, and not moving past an enemy. We put all of these rules in `ValorWorld`, so the controller just asks `ValorWorld` to move and gets true or false.
3. The terrain bonus has to change every time a hero changes its position, not only when it walks. Teleport, recall, and respawn also change the position. So all the position changes go through one method. It removes the old bonus and adds the new one, so the bonus never stacks.
4. Teleport has many rules. The hero needs to go to another lane, next to or behind a hero, not in front of it, not on another hero, and not past a monster. We return a list of the spaces the hero can go to, so the player can only choose from the right spaces.
5. Sometimes many monsters are in range, or many obstacles are next to the hero. At first the methods only returned one target, so we changed them to return a list and the player chooses one.
6. A failed action, a cancel, or a wrong command should not use the hero's turn. We only move to the next hero after a successful action. The market, help, and info also do not use the turn.
7. Heroes who die need to come back at their own Nexus, but another hero might be standing there. New monsters also need their Nexus space to be free. If the space is full, we keep them waiting and try again next round.
8. We combined two people's code. Both games use the same heroes, monsters, items, damage calculation, and markets, so we made `GameController` the parent of both controllers and kept the game-specific parts (`MhWorld`, `ValorWorld`, and the sessions) separate. We also had to make sure Monsters and Heroes still works after the changes.
9. The board has a lot of information in a small terminal. Every space is drawn as a 3-line cell like the example in the assignment, so the space type and the heroes and monsters inside it can be seen at the same time.
10. There are many random results in the game, so it is hard to repeat a problem. We use the same seed and the same input to get the same game again when testing.

## How to compile and run
---------------------------------------------------------------------------

### 1. Compile the game
From the project root directory (`MonstersAndHeroes/`):

```bash
javac -d bin -sourcepath src/main src/main/legends/Main.java
```

### 2. Run the game
```bash
java -cp bin legends.Main
```

The game first shows the game selector. Enter `2` to play Legends of Valor, `1` to play Monsters and Heroes, or `0` to exit.

Optional command-line flags:
- `--seed <integer>`: Run with a specific random seed for deterministic reproduction.
- `--no-color`: Disable ANSI color escape codes if your terminal does not support colors.
- `--data <directory>`: Specify an alternate data directory (defaults to `data`).

Example:
```bash
java -cp bin legends.Main --seed 42 --no-color
```

## Input/Output Example
---------------------------------------------------------------------------

```text
Input:
2
1
1
1
2
1
2
3
1
3

Output:
Select hero 1 (0 cancel) > Lane for Gaerdal_Ironhand (1 Top Lane, 2 Middle Lane, 3 Bottom Lane) >
...
Select hero 3 (0 cancel) > Lane for Parzival (1 Top Lane, 2 Middle Lane, 3 Bottom Lane) >
       0          1          2          3          4          5          6          7
   N - N - N  N - N - N  I - I - I  N - N - N  N - N - N  I - I - I  N - N - N  N - N - N
 0 |       |  |    M1 |  | X X X |  |       |  |    M2 |  | X X X |  |       |  |    M3 |
   N - N - N  N - N - N  I - I - I  N - N - N  N - N - N  I - I - I  N - N - N  N - N - N
...
   N - N - N  N - N - N  I - I - I  N - N - N  N - N - N  I - I - I  N - N - N  N - N - N
 7 | H1    |  |       |  | X X X |  | H2    |  |       |  | X X X |  | H3    |  |       |
   N - N - N  N - N - N  I - I - I  N - N - N  N - N - N  I - I - I  N - N - N  N - N - N

Legend: N=Nexus  I=Impassible  O=Obstacle  P=Plain  B=Bush  C=Cave  K=Koulou
        Top row = monsters' Nexus, bottom row = heroes' Nexus. H1=hero, M1=monster.
  H1   Gaerdal_Ironhand     (7, 0)  HP 125/125  MP 100/100
  H2   Rillifane_Rallathil  (7, 3)  HP 85/85  MP 1300/1300
  H3   Parzival             (7, 6)  HP 125/125  MP 300/300
  M1   Iron Colossus        (0, 1)  HP 200/200  Lv 1
  M2   Iron Colossus        (0, 4)  HP 200/200  Lv 1
  M3   Iron Colossus        (0, 7)  HP 200/200  Lv 1

=== Round 1 | H1 Gaerdal_Ironhand (Lv1 WARRIOR) | HP 125/125 | MP 100/100 | row 7, col 0 NEXUS ===
Attack power 376 | DEX 600 | Dodge 20%
W/A/S/D move, F attack, C spell, U potion, E equip, T teleport, R recall, O clear obstacle, B market, X pass, H help
H1 action >
```

```text
Input:
W
B
4
0
W
W

Output:
H2 action > Market | Rillifane_Rallathil | Gold 2500
1 buy, 2 sell, 3 repair, 4 inventory, 0 leave > Rillifane_Rallathil inventory:
  #2 Wand Lv1 price 80 durability 35/35 | damage 35, magic 16, hands 1
Market | Rillifane_Rallathil | Gold 2500
1 buy, 2 sell, 3 repair, 4 inventory, 0 leave > H2 action >
...
=== Monsters' turn ===
M1 advances to row 1, col 1.
M2 advances to row 1, col 4.
M3 advances to row 1, col 7.
```

```text
Input:
W
O
1
W

Output:
=== Round 2 | H2 Rillifane_Rallathil (Lv1 SORCERER) | HP 85/85 | MP 1300/1300 | row 6, col 3 BUSH ===
Attack power 393 | DEX 550 | Dodge 19%
W/A/S/D move, F attack, C spell, U potion, E equip, T teleport, R recall, O clear obstacle, B market, X pass, H help
H2 action > 1. row 5, col 3
2. row 6, col 4
Obstacle (0 cancel) > The obstacle at row 5, col 3 is now a plain space.
...
=== Monsters' turn ===
M1 advances to row 2, col 1.
M2 advances to row 2, col 4.
M3 advances to row 2, col 7.
```

```text
Input:
W
W
W
F
1

Output:
       0          1          2          3          4          5          6          7
...
   B - B - B  P - P - P  I - I - I  C - C - C  C - C - C  I - I - I  C - C - C  O - O - O
 3 |       |  |    M1 |  | X X X |  |       |  |    M2 |  | X X X |  |       |  |       |
   B - B - B  P - P - P  I - I - I  C - C - C  C - C - C  I - I - I  C - C - C  O - O - O

   K - K - K  C - C - C  I - I - I  C - C - C  K - K - K  I - I - I  K - K - K  C - C - C
 4 | H1    |  |       |  | X X X |  |       |  |       |  | X X X |  | H3    |  |       |
   K - K - K  C - C - C  I - I - I  C - C - C  K - K - K  I - I - I  K - K - K  C - C - C

   P - P - P  P - P - P  I - I - I  P - P - P  K - K - K  I - I - I  B - B - B  P - P - P
 5 |       |  |       |  | X X X |  | H2    |  |       |  | X X X |  |       |  |       |
   P - P - P  P - P - P  I - I - I  P - P - P  K - K - K  I - I - I  B - B - B  P - P - P
...
=== Round 4 | H1 Gaerdal_Ironhand (Lv1 WARRIOR) | HP 125/125 | MP 100/100 | row 4, col 0 KOULOU ===
Attack power 411 | DEX 600 | Dodge 20%
W/A/S/D move, F attack, C spell, U potion, E equip, T teleport, R recall, O clear obstacle, B market, X pass, H help
H1 action > 1. M1 Iron Colossus Lv1 HP 200/200 at row 3, col 1
Target (0 cancel) > Hero attack: Gaerdal_Ironhand -> Iron Colossus: 200 damage (critical) | HP 200 -> 0/200; defeated
M1 Iron Colossus is defeated!
Each surviving hero gains 80 gold and 5 EXP.
```

```text
Input:
W
W

Output:
=== Monsters' turn ===
M2 attacks Rillifane_Rallathil:
Enemy attack: Iron Colossus -> Rillifane_Rallathil: 23 damage | HP 150 -> 127/150
M3 attacks Parzival:
Enemy attack: Iron Colossus -> Parzival: dodged
```

```text
Input:
Q
Y
0

Output:
H1 action > Quit this game? Progress is not saved. Y yes, N/0 cancel > Returning to the game menu.

=== Select a Game to Play ===
1. Monsters and Heroes
2. Legends of Valor
0. Exit
Select a game >
```
