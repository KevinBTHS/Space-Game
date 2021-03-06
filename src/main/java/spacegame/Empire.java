package spacegame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

enum Affiliation {
    ALIEN {@Override public String toString() {return "Alien";}},
    HUMAN {@Override public String toString() {return "Human";}},
}

public class Empire { // Empire, along with all stats

    // private int galacticCoins = 0; // TODO: ADD MARKETPLACE
    private final Affiliation affiliation;

    private Empire target; // Primary target of bot
    private Empire coordinatedTarget; // Target if coordinated attack occurs
    public void setCoordinatedTarget(Empire empire) { this.coordinatedTarget = empire; }
    private int daysSinceLastAttack = 999;

    private String name;

    private final List<Planet> planets = new ArrayList<>();

    public Empire() { // Initialize your empire.
        System.out.println("What do you want to name your empire?");
        name = Game.scanner.nextLine();
        while(true) {
            System.out.println("Which galactic bloc do you want to fight for?");
            System.out.println("Alien | Human");
            String response = Game.scanner.nextLine();
            if(response.equalsIgnoreCase("alien")) {
                affiliation = Affiliation.ALIEN;
                Game.aliens.add(this);
                break;
            } else if(response.equalsIgnoreCase("human")) {
                affiliation = Affiliation.HUMAN;
                Game.humans.add(this);
                break;
            } else {
                System.out.println("Invalid input! Try again.");
            }
        }
        while(true) {
            Planet planet = Game.blackHole.getStar(Game.humanSystem).getRandomPlanet();
            if(planet.isColonized()) continue;
            planet.colonize(this, true);
            addPlanet(planet);
            break;
        }
    }

    public Empire(Affiliation affiliation) { // Initializes Bot Empire
        this.affiliation = affiliation;
        switch (affiliation) {
            case ALIEN:
                name = "Alien " + Game.aliens.size();
                Game.aliens.add(this);
                while (true) {
                    Planet planet = Game.blackHole.getStar(Game.humanSystem).getRandomPlanet();
                    if (planet.isColonized()) continue;
                    planet.colonize(this);
                    addPlanet(planet);
                    break;
                }
                break;
            case HUMAN:
                name = "Human " + Game.humans.size();
                Game.humans.add(this);
                Planet planet;
                while (true) {
                    planet = Game.blackHole.getStar(Game.alienSystem).getRandomPlanet();
                    if (planet.isColonized()) continue;
                    planet.colonize(this);
                    addPlanet(planet);
                    break;
                }
                break;
        }
    }

    void botStartActions() {
        for(Planet planet : planets) {
            switch (planet.getStatus()) {
                case ALERT: 
                    alert      (planet);
                    break;
                case DEFENSIVE: 
                    defensive  (planet);
                    break;
                case DEVELOP:
                    develop    (planet);
                    break;
                case OFFENSIVE: 
                    offensive  (planet);
                    break;
                case COORDINATED: 
                    coordinated(planet);
                    break;
            }
            planet.executeDefaultActions();
        }
    }
    
    private void alert(Planet planet) { // Mostly develop, build ships and defenses
        if(Math.random() < 0.75) { // TODO: Make sep algorithm. Too lazy.
            develop(planet);
        } else {
            int metalAmount = planet.getMetal();
            int crystalAmount = planet.getCrystal();
    
            if(planet.validAction() && planet.goalMet()) { // If planet goal is to build ship or defense, complete goal.
                planet.execute();
                planet.resetGoalStatus();
            }
            // TODO: Make better algorithm: Make build ships if defense is already good, all defenses if defense bad
            int availableMetal = (int) (metalAmount * 0.3); // Resources available for use
            int availableCrystal = (int) (crystalAmount * 0.3);
    
            int defenseMetal = (int) (0.5 * availableMetal); // Allocating specific amounts of metal for defenses
            int defenseCrystal = (int) (0.5 * availableCrystal);
            availableMetal = availableMetal - defenseMetal; // Rest of available metal will be used to make ships for counterattack
            availableCrystal = availableCrystal - defenseCrystal;
    
            List<Defense> defenses = new ArrayList<>(planet.getDefenses().keySet()); // List of defenses
            for(int i = defenses.size() - 1; i >= 0; i--) { // Tries to build building from best to least
                Defense defense = defenses.get(i);
                int metalCost = defense.getMetalCost();
                int crystalCost = defense.getCrystalCost();
                while(enoughMaterials(defenseMetal, defenseCrystal, defense)) { // Tries to build defenses until it can't anymore.
                    defense.build(planet);
                    defenseMetal = defenseMetal - metalCost;
                    defenseCrystal = defenseCrystal - crystalCost;
                }
            }
            availableMetal = availableMetal + defenseMetal;
            availableCrystal = availableCrystal + defenseCrystal;
    
            List<Ship> ships = new ArrayList<>(planet.getShips().keySet());
            for(int i = ships.size() - 1; i >= 0; i--) {
                Ship ship = ships.get(i);
                int metalCost = ship.getMetalCost();
                int crystalCost = ship.getCrystalCost();
                while(enoughMaterials(availableMetal, availableCrystal, ship)) {
                    ship.build(planet);
                    availableMetal = availableMetal - metalCost;
                    availableCrystal = availableCrystal - crystalCost;
                }
            }
        }
    }
    private void defensive(Planet planet) { // AI build defenses
        int metalAmount = planet.getMetal();
        int crystalAmount = planet.getCrystal();

        if(planet.validAction() && planet.goalMet()
        && planet.getActionType() == ActionType.OFFENSIVE) { // If planet goal is to build ship or defense, complete goal.
            planet.execute();
            planet.resetGoalStatus();
        }
        // TODO: Make better algorithm: Make build ships if defense is already good, all defenses if defense bad
        // Build more if defenses bad, build less if good.
        int availableMetal = (int) (metalAmount * 0.7); // Resources available for use
        int availableCrystal = (int) (crystalAmount * 0.7);

        int defenseMetal = (int) (0.5 * availableMetal); // Allocating specific amounts of metal for defenses
        int defenseCrystal = (int) (0.5 * availableCrystal);
        availableMetal = availableMetal - defenseMetal; // Rest of available metal will be used to make ships for counterattack
        availableCrystal = availableCrystal - defenseCrystal;

        List<Defense> defenses = new ArrayList<>(planet.getDefenses().keySet()); // List of defenses
        for(int i = defenses.size() - 1; i >= 0; i--) { // Tries to build building from best to least
            Defense defense = defenses.get(i);
            int metalCost = defense.getMetalCost();
            int crystalCost = defense.getCrystalCost();
            while(enoughMaterials(defenseMetal, defenseCrystal, defense)) { // Tries to build defenses until it can't anymore.
                defense.build(planet);
                defenseMetal = defenseMetal - metalCost;
                defenseCrystal = defenseCrystal - crystalCost;
            }
        }
        availableMetal = availableMetal + defenseMetal;
        availableCrystal = availableCrystal + defenseCrystal;

        List<Ship> ships = new ArrayList<>(planet.getShips().keySet());
        for(int i = ships.size() - 1; i >= 0; i--) {
            Ship ship = ships.get(i);
            int metalCost = ship.getMetalCost();
            int crystalCost = ship.getCrystalCost();
            while(enoughMaterials(availableMetal, availableCrystal, ship)) {
                ship.build(planet);
                availableMetal = availableMetal - metalCost;
                availableCrystal = availableCrystal - crystalCost;
            }
        }
    }
    private enum FocusBuilding {
        METAL,
        CRYSTAL,
        DEUTERIUM,
    }
    private void develop(Planet planet) {
        if(planet.validAction() && planet.goalMet()) {
            planet.execute();
            planet.resetGoalStatus();
        }
        FocusBuilding focusBuilding = FocusBuilding.METAL;
        if(planet.getDeuteriumMineLevel() < planet.getCrystalMineLevel() 
        && planet.getDeuteriumMineLevel() < planet.getMetalMineLevel())
            focusBuilding = FocusBuilding.DEUTERIUM;
        else if(planet.getCrystalMineLevel() < planet.getMetalMineLevel()
        || planet.getCrystalMineLevel() < planet.getDeuteriumMineLevel())
            focusBuilding = FocusBuilding.CRYSTAL;
        if(planet.getMetalMineLevel() < planet.getCrystalMineLevel()
        || planet.getMetalMineLevel() < planet.getDeuteriumMineLevel())
            focusBuilding = FocusBuilding.METAL;
        
        Building targetBuilding;
        switch (focusBuilding) {
            case METAL: targetBuilding = planet.getMetalMine();
                break;
            case CRYSTAL: targetBuilding = planet.getCrystalMine();
                break;
            case DEUTERIUM: targetBuilding = planet.getDeuteriumMine();
                break;
            default:
                System.out.println("ERROR: focusBuilding not set! developAction() + " + this);
                targetBuilding = planet.getMetalMine();
        }
        if(targetBuilding.enoughMaterials(planet)) {
            targetBuilding.levelUp(planet);
        } else { // Set goal to upgrade building
            planet.resetGoalStatus();
            planet.setGoal(
                () -> targetBuilding.levelUp(planet), ActionType.DEVELOP, 
                targetBuilding.getMetalCost(planet), 
                targetBuilding.getCrystalCost(planet), 
                targetBuilding.getDeuteriumCost(planet)
            );
        }
    }
    private void offensive(Planet planet) {
        int availableMetal = planet.getMetal();
        int availableCrystal = planet.getCrystal();
        
        // PHASE 1: Checks if action goal is an attack
        // If not, continue to PHASE 2
        if(planet.validAction() && planet.goalMet()
        && planet.getActionType() == ActionType.OFFENSIVE 
        || planet.getActionType() == ActionType.ATTACK) {
            planet.execute();
            if(planet.getActionType() == ActionType.ATTACK) {
                planet.resetGoalStatus();
                return;
                // If action is an attack, end
            }
            planet.resetGoalStatus();
        }
        // PHASE 2: Checks power difference and attempts to build ships and start attack.
        // TODO: When building ships, find defensive and offensive capabilities of ship. 
        // Make bot form smart formations once attack formations is in effect.
        int enemyDefensivePower = calcDefensivePower(findWeakestDefensivePlanet(target));
        List<Ship> ships = new ArrayList<>(planet.getShips().keySet());
        while(calcOffensivePower(planet) - 1 < enemyDefensivePower * 1.3 + 10) {
            for(int i = ships.size() - 1; i >= 0; i--) {
                Ship ship = ships.get(i);
                if(enoughMaterials(availableMetal, availableCrystal, ship)) {
                    ship.build(planet);
                    availableMetal = availableMetal - ship.getMetalCost();
                    availableCrystal = availableCrystal - ship.getCrystalCost();
                    break;
                }
            }
            if(availableMetal < Planet.fighters.getMetalCost() 
            || availableCrystal < Planet.fighters.getCrystalCost()) // If can't build anymore
                break;
        }
        if(calcOffensivePower(planet) - 1 > enemyDefensivePower * 1.3 + 10 && hasShips(planet)) { // Start attack if confident enough
            startAttack(planet, findWeakestDefensivePlanet(target)); 
            // Starts an attack on the weakest planet.
            // If deuterium is not enough, deuterium goal will be set.
            // Bot will continue trying to attack if not enough deuterium unless status change.
            // (Unlikely as bot will have lots of ships.)
        }
    }
            private Planet findWeakestDefensivePlanet(Empire empire) {
                List<Planet> empirePlanets = empire.getPlanets();
                if(empirePlanets.isEmpty()) {
                    empire.forceNewTarget();
                    return findWeakestDefensivePlanet(target);
                }
                Planet weakestPlanet = empirePlanets.get(0);
                int defensivePower = 0;
                for(Planet planet : empirePlanets) {
                    if(defensivePower < calcDefensivePower(planet)) {
                        weakestPlanet = planet;
                        defensivePower = calcDefensivePower(planet);
                    }
                }
                return weakestPlanet;
            }
    private void coordinated(Planet planet) {
        int availableMetal = planet.getMetal();
        int availableCrystal = planet.getCrystal();
        int enemyDefensivePower = calcDefensivePower(findWeakestDefensivePlanet(target));
        List<Ship> ships = new ArrayList<>(planet.getShips().keySet());
        // TODO: Better build algorithm:
        while(calcOffensivePower(planet) - 1 < enemyDefensivePower * 1.3 + 10) {
            for(int i = ships.size() - 1; i >= 0; i--) {
                Ship ship = ships.get(i);
                if(enoughMaterials(availableMetal, availableCrystal, ship)) {
                    ship.build(planet);
                    availableMetal = availableMetal - ship.getMetalCost();
                    availableCrystal = availableCrystal - ship.getCrystalCost();
                    break;
                }
            }
            if(availableMetal < Planet.fighters.getMetalCost() 
            || availableCrystal < Planet.fighters.getCrystalCost()) // If can't build anymore
                break;
        }
        if(hasShips(planet)) startAttack(planet, findWeakestDefensivePlanet(coordinatedTarget)); 
    }
    
    public boolean hasShips(Planet planet) {
        Map<Ship, Integer> ships = planet.getShips();
        for(Map.Entry<Ship, Integer> entry : ships.entrySet()) {
            if(entry.getValue() != 0 && entry.getKey().getAttack() != 0) return true;
        }
        return false;
    }
    
    public boolean enoughMaterials(int availableMetal, int availableCrystal, Ship ship) {
        return (availableMetal >= ship.getMetalCost() && availableCrystal >= ship.getCrystalCost());
    }
    public boolean enoughMaterials(int defenseMetal, int defenseCrystal, Defense defense) {
        return defenseMetal >= defense.getMetalCost() && defenseCrystal >= defense.getCrystalCost();
    }
    
    private int[] enemyAveragePower() {
        int enemyOffensivePower = 0;
        int enemyDefensivePower = 0;
        int enemyPotentialPower = 0;
        for(Planet planet : target.getPlanets()) {
            enemyOffensivePower = enemyOffensivePower + calcOffensivePower(planet);
            enemyDefensivePower = enemyDefensivePower + calcDefensivePower(planet);
            enemyPotentialPower = enemyPotentialPower + calcPotentialPower(planet);
        }
        // Calcs averages
        enemyOffensivePower = (int) ((double) enemyOffensivePower / target.getPlanets().size());
        enemyDefensivePower = (int) ((double) enemyDefensivePower / target.getPlanets().size());
        enemyPotentialPower = (int) ((double) enemyPotentialPower / target.getPlanets().size());
        return new int[] {enemyOffensivePower, enemyDefensivePower, enemyPotentialPower};
    }

    public void statusCheck() {
        int[] enemyPower = enemyAveragePower();
        int enemyOffensivePower = enemyPower[0];
        int enemyPotentialPower = enemyPower[2];
        for(Planet planet : planets) {
            int totalOffensivePower = calcOffensivePower(planet);
            int totalDefensivePower = calcDefensivePower(planet);
            int totalPotentialPower = calcPotentialPower(planet);
            if(enemyOffensivePower > totalDefensivePower + totalPotentialPower * 0.5) {
                // If enemy offensive power is greater than our defensive power and half of potential powers
                if(totalPotentialPower + totalDefensivePower < enemyOffensivePower) {
                    // If enemy offensive power is unstoppable
                    planet.setStatus(Status.DEFENSIVE);
                } else {
                    // If enemy offensive power is stoppable
                    planet.setStatus(Status.ALERT);
                }
            } else {
                // Enemy offensive power is less than our defensive power
                if(enemyPotentialPower > totalPotentialPower) {
                    // If enemy potential power greater than our potential power
                    planet.setStatus(Status.DEVELOP);
                } else if(totalOffensivePower + totalPotentialPower * 0.4 > calcDefensivePower(findWeakestDefensivePlanet(target))) {
                    // If our potential offensive power is greater than enemy's weakest planet
                    planet.setStatus(Status.OFFENSIVE);
                } else {
                    // If enemy's weakest planet is armored
                    planet.setStatus(Status.DEVELOP);
                }
            }
            for(Expedition expedition : Game.expeditions) { // Checks if any incoming attacks.
                if(expedition.checkAttacks(planet)) {
                    this.target = expedition.getAttacker().getEmpire();
                    setDaysSinceLastAttack(0);
                    int enemyAttackOffensivePower = Empire.calcOffensivePower(expedition.getFleet());
                    int defensivePower = Empire.calcDefensivePower(planet);
                    if(enemyAttackOffensivePower > defensivePower) {
                        planet.setStatus(Status.DEFENSIVE);
                    } else {
                        planet.setStatus(Status.ALERT);
                    }
                }
            }
        }
    }

    void newTarget() { // Selects new primary target.
        if((Game.random.nextInt(15) == 0 && getDaysSinceLastAttack() > 15)
        || target == null || target.checkDestroyed()) {
            switch (affiliation) {
                case ALIEN: 
                    target = Game.humans.get(Game.random.nextInt(Game.humans.size()));
                    break;
                case HUMAN:
                    target = Game.aliens.get(Game.random.nextInt(Game.aliens.size()));
                    break;
            }
        }
    }
    public void forceNewTarget() {
        switch (affiliation) {
            case ALIEN:
                if(Game.humans.isEmpty()) System.out.println("ERROR: Humans empty, cannot get new target! forceNewTarget()");
                target = Game.humans.get(Game.random.nextInt(Game.humans.size()));
                break;
            case HUMAN:
                if(Game.aliens.isEmpty()) System.out.println("ERROR: Aliens empty, cannot get new target! forceNewTarget()");
                target = Game.aliens.get(Game.random.nextInt(Game.humans.size()));
                break;
        }
    }

    private static int calcOffensivePower(Planet planet) { // Calculates offensive threat of planet
        return (int) (
            planet.getShipCount(Planet.fighters    ) * Planet.fighters   .getOffensivePower() +
            planet.getShipCount(Planet.destroyers       ) * Planet.destroyers      .getOffensivePower() +
            planet.getShipCount(Planet.tanks            ) * Planet.tanks           .getOffensivePower() +
            planet.getShipCount(Planet.hijackers        ) * Planet.hijackers       .getOffensivePower() +
            planet.getShipCount(Planet.missileLaunchers ) * Planet.missileLaunchers.getOffensivePower() +
            planet.getShipCount(Planet.battleships      ) * Planet.battleships     .getOffensivePower()
            );
    }
    private static int calcOffensivePower(Map<Ship, Integer> fleet) {
        int total = 0;
        for(Map.Entry<Ship, Integer> entry : fleet.entrySet()) {
            Ship ship = entry.getKey();
            int count = entry.getValue();
            total = (int) (total + ship.getOffensivePower() * count);
        }
        return total;
    }
    private static int calcDefensivePower(Planet planet) {
        return (int) (
            0.8 * ( // Ships weaker when defending
                planet.getShipCount(Planet.fighters    ) * Planet.fighters   .getDefensivePower() +
                planet.getShipCount(Planet.destroyers       ) * Planet.destroyers      .getDefensivePower() +
                planet.getShipCount(Planet.tanks            ) * Planet.tanks           .getDefensivePower() +
                planet.getShipCount(Planet.hijackers        ) * Planet.hijackers       .getDefensivePower() +
                planet.getShipCount(Planet.missileLaunchers ) * Planet.missileLaunchers.getDefensivePower() +
                planet.getShipCount(Planet.battleships      ) * Planet.battleships     .getDefensivePower()
                ) +
                planet.getDefenseCount(Planet.missileCannon       ) * Planet.missileCannon      .getDefensivePower() +
                planet.getDefenseCount(Planet.laserCannon         ) * Planet.laserCannon        .getDefensivePower() +
                planet.getDefenseCount(Planet.plasmaCannon        ) * Planet.plasmaCannon       .getDefensivePower() +
                planet.getDefenseCount(Planet.particleAccelerator ) * Planet.particleAccelerator.getDefensivePower()
        );
    }
    private static int calcPotentialPower(Planet planet) { // Calculates the potential offensive threat / defensive power of planet
        int levels = planet.getMetalMineLevel() + planet.getCrystalMineLevel() + planet.getDeuteriumMineLevel();
        int resourceCount = planet.getMetal() + planet.getCrystal() * 2 + planet.getDeuterium() * 3;
        return (levels / 3) * (resourceCount / 1200);
    }

    public void checkAttacks() {
        for(Expedition expedition : Game.expeditions) {
            if(expedition.checkAttacks(Game.player)) {
                System.out.println("Your planet " + expedition.getDefender() + " is being attacked!");
                System.out.println("Attacker: " + expedition.getAttacker().getEmpire() + "'s planet " + expedition.getAttacker());
                System.out.println("Time Left Until Attack: " + expedition.getTimeLeft());
            }
        }
    }

    public void playerEmpireStartActions() {
        for(Planet planet : planets) {
            playerStartActions(planet);
            planet.executeDefaultActions();
        }
    }

    private void playerStartActions(Planet planet) {
        System.out.println();
        System.out.println(planet.getFullInfo());
        System.out.println("Metal: " + planet.getMetal());
        System.out.println("Crystal: " + planet.getCrystal());
        System.out.println("Deuterium: " + planet.getDeuterium());
        System.out.println();
        System.out.println("What would you like to do on " + planet.getName() + "?");
        System.out.println("Build | Attack | Map | Stats | Info | Continue");
        System.out.println();
        String response = Game.scanner.nextLine();

        // TODO: Add research NOT!!!
        if(response.equalsIgnoreCase("build")) {
            promptBuild(planet);
        } else if(response.equalsIgnoreCase("attack")) {
            promptAttack(planet);
        } else if(response.equalsIgnoreCase("map")) {
            showMap();
        } else if(response.equalsIgnoreCase("stats")) {
            printStats(planet);
        } else if(response.equalsIgnoreCase("info")) {
            printInfo();
        } else if(response.equalsIgnoreCase("continue")) { // Continue is the only way to break out of recursion.
            return;
        } else if(response.equalsIgnoreCase("Hello World")) { // If you see this, stop cheating
            planet.addMetal(50000);
            planet.addCrystal(50000);
            planet.addDeuterium(50000);
        } else {
            System.out.println("You did not enter a valid response! Try again.");
            System.out.println();
        }
        playerStartActions(planet);
    }
            private void promptBuild(Planet planet) {
                System.out.println();
                System.out.println("What would you like to build on " + planet.getName() + "?");
                System.out.println("(1) Build / Level Up | (2) Build Ships | (3) Build Defenses");
                System.out.println();
                String response = Game.scanner.nextLine();

                switch (response) {
                    case "1":
                    case "(1)": levelUp(planet);
                        break;
                    case "2":
                    case "(2)": promptBuildShips(planet);
                        break;
                    case "3":
                    case "(3)": promptBuildDefenses(planet);
                        break;
                    default:
                        System.out.println("You did not enter a valid response!");
                        System.out.println();
                }
            }
            private void levelUp(Planet planet) {
                Set<Building> buildings = planet.getBuildings().keySet();
                System.out.println();
                System.out.println("What would you like to build / level up?");
                System.out.println("Type out the building name for more information. | \"Back\" to go back.");
                System.out.println();
                for(Building building : buildings) {
                    System.out.println(building + " at level: " + planet.getLevel(building));
                }
                System.out.println();
                String response = Game.scanner.nextLine();
                if(response.equalsIgnoreCase("back")) {
                    return;
                }
                for(Building building : buildings) {
                    if(response.equalsIgnoreCase(building.toString())) {
                        System.out.println();
                        System.out.println("Current Level of Building: " + planet.getLevel(building));
                        printBuildingCost(building, planet);
                        System.out.println("Do you want to level up this building?");
                        System.out.println("Yes | No");
                        System.out.println();
                        response = Game.scanner.nextLine();

                        if(response.equalsIgnoreCase("yes")) {
                            building.levelUp(planet);
                        }
                        return;
                    }
                }
                System.out.println("Building not found!");
                levelUp(planet);
            }
            private void promptBuildShips(Planet planet) {
                System.out.println();
                System.out.println("Which ship would you like to build?");
                System.out.println("Type out the name of the ship for more information. | \"Back\" to go back.");
                System.out.println();
                for(Map.Entry<Ship, Integer> entry : planet.getShips().entrySet()) {
                    System.out.println(entry.getKey());
                }
                System.out.println();
                String response = Game.scanner.nextLine();
                if(response.equalsIgnoreCase("back")) {
                    return;
                }

                for(Map.Entry<Ship, Integer> entry : planet.getShips().entrySet()) {
                    Ship ship = entry.getKey();
                    if(response.equalsIgnoreCase(ship.toString())) {
                        System.out.println();
                        System.out.println(ship.toString());
                        System.out.println(ship.getDescription());
                        ship.printInfo();
                        System.out.println("You currently have " + planet.getShipCount(ship) + " " + ship.toString() + ".");
                        System.out.println();
                        ship.build(planet);
                        System.out.println();
                        return;
                    }
                }
                System.out.println("Ship not found! Try again.");
                promptBuildShips(planet);
            }
            private void promptBuildDefenses(Planet planet) {
                System.out.println();
                System.out.println("Which defense would you like to build?");
                System.out.println("Type out the name of the defense for more information. | \"Back\" to go back.");
                System.out.println();
                for(Map.Entry<Defense, Integer> entry : planet.getDefenses().entrySet()) {
                    System.out.println(entry.getKey());
                }
                System.out.println();
                String response = Game.scanner.nextLine();
                if(response.equalsIgnoreCase("back")) {
                    return;
                }

                for(Map.Entry<Defense, Integer> entry : planet.getDefenses().entrySet()) {
                    Defense defense = entry.getKey();
                    if(response.equalsIgnoreCase(defense.toString())) {
                        System.out.println();
                        System.out.println(defense.toString());
                        System.out.println(defense.getDescription());
                        defense.printInfo();
                        System.out.println("You currently have " + planet.getDefenseCount(defense) + " " + defense.toString() + ".");
                        System.out.println();
                        defense.build(planet);
                        System.out.println();
                        return;
                    }
                }
                System.out.println("Defense not found! Try again.");
                promptBuildDefenses(planet);
            }
            private void printBuildingCost(Building building, Planet planet) {
                if(building.getMetalCost(planet) != 0) System.out.println("Metal Cost: " + building.getMetalCost(planet));
                if(building.getCrystalCost(planet) != 0) System.out.println("Crystal Cost: " + building.getCrystalCost(planet));
                if(building.getDeuteriumCost(planet) != 0) System.out.println("Deuterium Cost: " + building.getDeuteriumCost(planet));
            }

    private void promptAttack(Planet planet) { // TODO: Create better algorithm
        // TODO: Stop people from attacking if no ships
        // TODO: Refactor this
        System.out.println();
        System.out.println("Which planet would you like to attack?");
        System.out.println("You can give the location in indices or coordinates.");
        System.out.println("Index Format: \"1:6\" (\"Star Index:Planet Index\")");
        System.out.println("Coordinates: \"34, -9, 66\" (\"x, y, z\")");
        System.out.println();
        String response = Game.scanner.nextLine();

        int[] targetCoordinates = new int[3];
        String targetIndex = "";

        if(response.contains(":")) { // Index Format
            targetIndex = response.replaceAll("\\s", ""); //Removes all whitespace
        } else { // Coordinate format
            String[] tempStrings = response.split(",[ ]*"); //Splits string by comma and whitespace
            try {
                for(int i = 0; i < 3; i++) {
                    targetCoordinates[i] = Integer.parseInt(tempStrings[i]);
                }
            } catch (Exception e) {
                System.out.println("ERROR: Invalid coordinates / indices!");
                return;
            }
        }

        if(targetIndex.equals("")) { // Coordinates
            for(Empire empire : Game.allEmpires()) {
                for(Planet enemyPlanet : empire.getPlanets()) {
                    if(Arrays.equals(enemyPlanet.getCoordinates(), targetCoordinates)) {
                        startAttack(planet, enemyPlanet);
                        return;
                    }
                }
            }
        } else { // Indices
            for(Empire empire : Game.allEmpires()) {
                for(Planet enemyPlanet : empire.getPlanets()) {
                    if(enemyPlanet.getIndex().equals(targetIndex)) {
                        startAttack(planet, enemyPlanet);
                        return;
                    }
                }
            }
        }
        System.out.println("Planet not found!");
    }

    private void startAttack(Planet homePlanet, Planet enemyPlanet) { // TODO: REFACTOR THIS
        if(this != Game.player) {
            botStartAttack(homePlanet, enemyPlanet); // If bot
            return;
        }
        System.out.println();
        System.out.println("You are about to attack planet " + enemyPlanet.getFullInfo() + " belonging to " + enemyPlanet.getEmpire() + ".");
        System.out.println("How many ships would you like to use to attack this planet?");
        System.out.println();
        Map<Ship, Integer> ships = homePlanet.getShips();
        Map<Ship, Integer> fleet = new LinkedHashMap<>();
        while(true) {
            boolean found = false;

            for(Map.Entry<Ship, Integer> entry : ships.entrySet()) {
                if(entry.getValue() != 0) System.out.println("You have " + entry.getValue() + " " + entry.getKey() + ".");
            }
            System.out.println("Type out the name of the ship you want to attack with | \"Proceed\" to proceed to the next stage of the attack.");
            System.out.println();
            String response = Game.scanner.nextLine();

            if(response.equals("proceed")) break;

            for(Map.Entry<Ship, Integer> entry : ships.entrySet()) {
                if(response.equalsIgnoreCase(entry.getKey().getName())) {
                    found = true;
                    if(entry.getValue() == 0) {
                        System.out.println("You don't have any " + entry.getKey().getName() + "!");
                        break;
                    }
                    int amount;
                    System.out.println();
                    System.out.println("You have " + entry.getValue() + " " + entry.getKey() + ".");
                    System.out.println("How many ships would you like to use?");
                    System.out.println();
                    try {
                        amount = Integer.parseInt(Game.scanner.nextLine());
                    } catch (Exception e) {
                        System.out.println("ERROR: Invalid input!");
                        break;
                    }
                    if(amount > entry.getValue()) {
                        System.out.println("You do not have enough ships to complete this operation!");
                        break;
                    }
                    if(amount < 1) {
                        System.out.println("You must add 1 or more ships.");
                        break;
                    }
                    fleet.put(entry.getKey(), amount);
                }
            }
            if(!found) {
                System.out.println("Ship not found! Try again.");
            }
        }
        int deuteriumCost = calcDeuteriumCost(homePlanet, enemyPlanet, fleet);
        int flightTime = calcFlightTime(homePlanet, enemyPlanet, fleet);

        if(deuteriumCost > homePlanet.getDeuterium()) {
            System.out.println();
            System.out.println("You do not have enough deuterium to complete this operation!");
            System.out.println("Your planet currently has " + homePlanet.getDeuterium() + " deuterium.");
            System.out.println("You need " + deuteriumCost + " deuterium to complete this operation.");
            System.out.println();
            return;
        }
        
        System.out.println();
        System.out.println("Your planet currently has " + homePlanet.getDeuterium() + " deuterium.");
        System.out.println("You need " + deuteriumCost + " deuterium to complete this operation.");
        System.out.println("It takes you " + flightTime + " days to complete this operation.");
        System.out.println("Would you like to proceed? Yes | No");
        System.out.println();

        String response = Game.scanner.nextLine();

        if(!response.equalsIgnoreCase("yes")) {
            return;
        }

        System.out.println("You are about to create a new attack.");
        Game.expeditions.add(new Expedition(homePlanet, enemyPlanet, fleet, Expedition.ExpeditionType.ATTACK)); // Makes a new attack
        homePlanet.attackSubtract(deuteriumCost, fleet);
        System.out.println("Attack successfully created!");
    }
    private void botStartAttack(Planet homePlanet, Planet enemyPlanet) {
        Map<Ship, Integer> fleet = homePlanet.getShips();
        int deuteriumCost = calcDeuteriumCost(homePlanet, enemyPlanet, fleet);
        if(deuteriumCost > homePlanet.getDeuterium()) {
            homePlanet.resetGoalStatus();
            // If not enough deuterium to attack, sets goal and then attacks
            homePlanet.setGoal(
                () -> this.botStartAttack(homePlanet, enemyPlanet),
                ActionType.ATTACK, 
                0, 0, deuteriumCost
            );
            return;
        }
        Game.expeditions.add(new Expedition(homePlanet, enemyPlanet, fleet,
                Expedition.ExpeditionType.ATTACK));
        homePlanet.attackSubtract(deuteriumCost, fleet);
    }
            public static int calcDeuteriumCost(Planet planet1, Planet planet2, Map<Ship, Integer> fleet) {
                double totalCost = 0;
                double distance = Coordinates.getDistance(planet1.getCoordinates(), planet2.getCoordinates());
                for(Map.Entry<Ship, Integer> entry : fleet.entrySet()) {
                    Ship ship = entry.getKey();
                    int count = entry.getValue();
                    totalCost = totalCost + 60 * count + ship.getFuelMultiplier() * 25 * distance * count;
                }
                if(planet1.getEmpire() != Game.player) return (int) totalCost / 2;
                return (int) totalCost;
            }
            public static int calcFlightTime(Planet planet1, Planet planet2, Map<Ship, Integer> fleet) {
                double distance = Coordinates.getDistance(planet1.getCoordinates(), planet2.getCoordinates());
                int totalShips = 0;
                double totalSpeed = 0.0;
                for(Map.Entry<Ship, Integer> entry : fleet.entrySet()) {
                    totalShips = totalShips + entry.getValue();
                    totalSpeed = totalSpeed + entry.getKey().getSpeedMultiplier() * entry.getValue();
                }
                if(totalShips == 0) {
                    System.out.println("ERROR: Division by 0 in calcFlightTime(). Adjusted to 1.");
                    totalShips = 1;
                }
                double averageSpeed = totalSpeed / totalShips;
                return (int) ((averageSpeed * distance) / 100) + 1; // 2 Days minimum, 4 max | 50) + 2 |
            }

    private void showMap() {
        System.out.println();
        System.out.println("Which party do you want to display planets for?");
        System.out.println("Aliens | Humans | All");
        System.out.println();
        String response = Game.scanner.nextLine();
        System.out.println();

        if(response.equalsIgnoreCase("aliens") || response.equalsIgnoreCase("alien")) {
            System.out.println("aliens:");
            printPlanets(Game.aliens);
        } else if(response.equalsIgnoreCase("humans") || response.equalsIgnoreCase("human")) {
            System.out.println("Humans:");
            printPlanets(Game.humans);
        } else if(response.equalsIgnoreCase("all")) {
            System.out.println("Aliens:");
            printPlanets(Game.aliens);
            System.out.println("Humans:");
            printPlanets(Game.humans);
        } else {
            System.out.println("Invalid input!");
        }
    }
            private void printPlanets(List<Empire> empire) {
                for(Empire indexEmpire : empire) {
                    System.out.println("Empire: " + indexEmpire.toString());
                    for(Planet planet : indexEmpire.getPlanets()) {
                        System.out.println(planet.getFullInfo());
                    }
                    System.out.println();
                }
            }
    
    private void printStats(Planet planet) {
        System.out.println();
        System.out.println("Metal: " + planet.getMetal());
        System.out.println("Crystal: " + planet.getCrystal());
        System.out.println("Deuterium: " + planet.getDeuterium());
        System.out.println();
    }

    private void printInfo() {
        System.out.println();
        System.out.println("Build: Build new ships or defenses or upgrade existing buildings.");
        System.out.println("Attack: Attack an enemy planet.");
        System.out.println("Map: Displays a map of all other planets.");
        System.out.println("Info: Displays these messages.");
        System.out.println("Continue: Moves on to the next planet / End your turn.");
        System.out.println();
    }

    public void addPlanet(Planet planet) {
        planets.add(planet);
    }
    public void remPlanet(Planet planet) {
        planets.remove(planet);
    }
    public List<Planet> getPlanets() {
        return planets;
    }
    
    public boolean checkDestroyed() { // If empire has no planets left 
        if(planets.isEmpty()) {
            Game.aliens.remove(this);
            Game.humans.remove(this);
            return true;
        }
        return false;
    }

    public int getDaysSinceLastAttack() {return daysSinceLastAttack;}
    public void setDaysSinceLastAttack(int i) {daysSinceLastAttack = i;}

    public Affiliation getAffiliation() {return affiliation;}
    public String toString() {return name;}
    public String getName() {return name;}
}