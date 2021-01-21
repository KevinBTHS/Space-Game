import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

enum ActionType {
    OFFENSIVE,
    ATTACK,
    DEVELOP,
}

enum Status { // status that enemy algorithm is in
    DEFENSIVE, // Algorithm knows you're attacking / have the ships to do so.
    ALERT, // Algorithm is aware that you can attack.
    OFFENSIVE, // Algorithm is seeking to attack.
    DEVELOP, // Algorithm is seeking to develop.
    COORDINATED, // Algorithm is coordinating to attack together.
}

public class Planet extends Coordinates { //Individual planet, resource amount

    // Base values upon generation.
    private static final int BASE_METAL     = 5_000_000; //5 mil
    private static final int BASE_CRYSTAL   = 2_500_000; //2.5 mil
    private static final int BASE_DEUTERIUM = 1_666_666; //1.67 mil
    // Base multipliers for mining materials
    private static final double BASE_METAL_MULTIPLIER     = 10;
    private static final double BASE_CRYSTAL_MULTIPLIER   = 10;
    private static final double BASE_DEUTERIUM_MULTIPLIER = 10;

    private Status status;
    public Status getStatus() {return status;}
    public void setStatus(Status status) {this.status = status;}
    
    private String name;
    private Empire empire;
    private final Star star;
    private final int planetIndex; // Index of the planet
    private boolean colonized = false; // TODO: Add colonization
    private boolean initiated = false;
    private boolean destroyed = false;

    private int temperature; // Temperature of the planet, affects many generation properties.

    // Unmined resources and multipliers
    private int unminedMetal;
    private double metalMultiplier;
    private int unminedCrystal;
    private double crystalMultiplier;
    private int unminedDeuterium;
    private double deuteriumMultiplier;

    // Local amount of resources on planet.
    private int localMetal = 18_000;
    private int localCrystal = 9_000;
    private int localDeuterium = 4_500;

    // Goal amount of resources that bot wants. TODO:
    private int metalGoal;
    private int crystalGoal;
    private int deuteriumGoal;
    private Action goalAction; // What to execute when goal is met // TODO: Make it so that goals are not overrided
    private ActionType actionType;

    public interface Action {void execute();}
    public void execute() {goalAction.execute();}
    public Action getGoalAction() {return goalAction;}
    public void setGoalAction(Action action) {goalAction = action;}
    public ActionType getActionType() {return actionType;}
    public void setActionType(ActionType actionType) {this.actionType = actionType;}
    
    public void resetGoalStatus() {
        metalGoal = 0;
        crystalGoal = 0;
        deuteriumGoal = 0;
        goalAction = null;
        actionType = null;
    }
    public boolean validAction() {
        if(this.goalAction == null) {
            if(this.actionType == null) {
                return false;
            }
            System.out.println("ERROR: goalAction unspecified but actionType specified.");
            return false;
        } else {
            if(this.actionType == null) {
                System.out.println("ERROR: goalAction specified but actionType unspecified.");
                return false;
            }
            return true;
        }
    }
    public boolean goalMet() {return (localMetal >= metalGoal && localCrystal >= crystalGoal && localDeuterium >= deuteriumGoal);}

    public Planet(Star star, int index) { // Makes a planet using a star and the position of the planet to the star.
        // Max coordinate is 50, minimum is -50.
        // The furthest a planet can be from the sun is 5.
        setCoordinate   (
            (int) ((Math.random() * 11) - 5 + star.getX()),
            (int) ((Math.random() * 11) - 5 + star.getY()),
            (int) ((Math.random() * 11) - 5 + star.getZ())
        );
        this.star = star;
        this.planetIndex = index;
        this.name = star.getSystemIndex() + ":" + planetIndex;
    }
    // TODO:
    // IMPORTANT: Make sure 2 planets cannot be on top of one another.
    // IMPORTANT: Make sure to make the planet closer or further away according to the index that the planet is at.
    // Should be relatively simple to do, Math.random() * 11/ 10 etc.
    
    public void initiate() {
        setTemperature(this.planetIndex);
        setBaseMaterialValues();
        initiated = true;
    }

    public void colonize(Empire empire) { // Sep colonize method to save memory TODO: Add colonization
        if(!initiated) initiate();
        this.empire = empire;
        status = Status.DEVELOP;
        initializeBuildings();
        initializeDefenses();
        initializeShips();
        colonized = true;
        if(empire == Game.player || this.empire == Game.player) {
            System.out.println("You colonized this planet!");
            System.out.println("Would you like to give a new name to this planet?");
            System.out.println("Default name: " + name + " (The indices of the planet.)");
            System.out.println("Yes | No");
            String response = Game.scanner.nextLine();
            if(!response.equalsIgnoreCase("yes")) return;
            System.out.println("Please enter the name for your planet:");
            name = Game.scanner.nextLine();
        }
    }
    public void colonize(Empire empire, boolean firstRun) {
        if(!initiated) initiate();
        this.empire = empire;
        status = Status.DEVELOP;
        initializeBuildings();
        initializeDefenses();
        initializeShips();
        colonized = true;
        if(firstRun) {
            levelUpBuildings();
            System.out.println("You colonized this planet!");
            System.out.println("Would you like to give a new name to this planet?");
            System.out.println("Default name: " + name + " (The indices of the planet.)");
            System.out.println("Yes | No");
            String response = Game.scanner.nextLine();
            if(!response.equalsIgnoreCase("yes")) return;
            System.out.println("Please enter the name for your planet:");
            name = Game.scanner.nextLine();
        }
    }
    public void levelUpBuildings() {
        metalMine.levelUp();
        crystalMine.levelUp();
        deuteriumMine.levelUp();
        solarPanel.levelUp();
    }
    
    public void destroy() {
        if(this.empire == Game.player) System.out.println("Your planet " + name + " was destroyed!");
        empire.remPlanet(this);
        empire.checkDestroyed();
        name = null;
        empire = null;
        colonized = false;
        destroyed = true;
    }

    public void setTemperature(int index) {
        // Setting temperature based on distance between sun and planet
        // Index should be between 0 and 9.
        // Temperature should be from -10 C to 30 C
        // Temperature affects resource generation and obtaining resources.
        temperature = (int) ((index + 1) * (Math.random() * 4)) - 10; // Get random number between 0 and 4 and multiply by 1 - 10, then subtract 10.
    }

    public void setBaseMaterialValues() { // Setting base values of multipliers and resources
        unminedMetal          = BASE_METAL + (BASE_METAL / 100) * (temperature > 10 ? (temperature - 10) : 0);
        // Ranges of metal on planet is from 5M to 6M (30) with a base value of 5M.
        unminedCrystal        = BASE_CRYSTAL + (BASE_CRYSTAL / 100) * (-1 * Math.abs(temperature - 10) + 20);
        // Ranges of crystal on planet is from 2.5M to 3M (10) with a base value of 2.5M.
        unminedDeuterium      = BASE_DEUTERIUM + (BASE_DEUTERIUM / 100) * (temperature < 10 ? -1 * (temperature - 10) : 0);
        // Ranges of deuterium on planet is from 1.67M to 2M (-10) with a base value of 1.67M
        //Graph Visible Here: https://www.desmos.com/calculator/vtwslahw5x

        metalMultiplier     = BASE_METAL_MULTIPLIER + BASE_METAL_MULTIPLIER * (temperature > 10 ? (temperature - 10) / 200.0 : 0);
        // Metal Multiplier, 1.0 - 1.1, base 1.0 (Closer to 30, bigger multiplier)
        crystalMultiplier   = BASE_CRYSTAL_MULTIPLIER + BASE_CRYSTAL_MULTIPLIER * (-2 * Math.abs(temperature - 10) / 400.0) + 0.1;
        // Crystal Multiplier, 1.0 - 1.1, base 1.0 (Closer to 10, bigger multiplier)
        deuteriumMultiplier = BASE_DEUTERIUM_MULTIPLIER + BASE_DEUTERIUM_MULTIPLIER * (temperature < 10 ? (temperature - 10) / -200.0 : 0);
        // Deuterium Multiplier, 1.0 - 1.1, base 1.0 (Closer to -10, bigger multiplier)
        // Graph Visible Here: https://www.desmos.com/calculator/j3nzsmwg1q
        
        unminedMetal     = (int) (unminedMetal + unminedMetal * Math.random());
        unminedCrystal   = (int) (unminedCrystal + unminedCrystal * Math.random());
        unminedDeuterium = (int) (unminedDeuterium + unminedDeuterium * Math.random());
    }

    public static Ship lightFighters    = new Ship("Light Fighters"   , 4000 , 3000 , 2000 , 1000 , 1     , 1.5);
    public static Ship smallCargoShips  = new Ship("Small Cargo Ships", 6000 , 1000 , 3000 , 0    , 10000      );
    public static Ship heavyFighters    = new Ship("Heavy Fighters"   , 8000 , 6500 , 4000 , 2500 , 1.2  , 0.8 );
    public static Ship destroyers       = new Ship("Destroyers"       , 10000, 12000, 5000 , 5500 , 1.3  , 0.9 );
    public static Ship tanks            = new Ship("Tanks"            , 15000, 6000 , 8000 , 2250 , 1.7  , 0.4 );
    public static Ship largeCargoShips  = new Ship("Large Cargo Ships", 18000, 1000 , 6000 , 0    , 40000      );
    public static Ship hijackers        = new Ship("Hijackers"        , 4000 , 10000, 1000 , 6000 , 1.5  , 2.5 );
    public static Ship missileLaunchers = new Ship("Missile Launchers", 25000, 20000, 15000, 13000, 1.8  , 0.6 );
    public static Ship battleships      = new Ship("Battleships"      , 50000, 40000, 30000, 28000, 2.7  , 0.4 );
    static { // Initialize attributes for ships
        destroyers.setMultiplier(1, 1.4);
        hijackers.setMultiplier(1.7, 1);
        missileLaunchers.setMultiplier(1, 1.4);
        battleships.setMultiplier(1.4, 1);

        lightFighters   .setLevelRequired(1); lightFighters   .setPower(1 , 0.8);
        smallCargoShips .setLevelRequired(1); smallCargoShips .setPower(0 , 0  );
        heavyFighters   .setLevelRequired(2); heavyFighters   .setPower(2 , 1.6);
        destroyers      .setLevelRequired(3); destroyers      .setPower(4 , 3.2);
        tanks           .setLevelRequired(4); tanks           .setPower(5 , 3  );
        largeCargoShips .setLevelRequired(4); largeCargoShips .setPower(0 , 0  );
        hijackers       .setLevelRequired(5); hijackers       .setPower(6 , 6  );
        missileLaunchers.setLevelRequired(6); missileLaunchers.setPower(10, 8.4);
        battleships     .setLevelRequired(7); battleships     .setPower(20, 18 );

        lightFighters.setDescription("Small mobile ships, faster but weaker than its counterparts.");
        smallCargoShips.setDescription("Cargo Ships, excelling at transporting cargo over long distances.");
        heavyFighters.setDescription("Armored ships with better firepower in exchange for speed and fuel consumption.");
        destroyers.setDescription("Powerful ships that excel at destroying enemy defenses.");
        tanks.setDescription("Slow and heavily armored ships that can survive even the most powerful attacks.");
        largeCargoShips.setDescription("Larger cargo ships, can hold more than 3x more than its smaller counterpart.");
        hijackers.setDescription("Extremely agile but fragile ships with serious firepower.");
        missileLaunchers.setDescription("Ships equipped with missiles specializing in destroying defenses.");
        battleships.setDescription("The titans of the battlefield, battleships are powerful, armored ships capable of destroying anything.");
    }
    private Map<Ship, Integer> ships; // Stores amount of ships of each type
    private void initializeShips() { // Putting ships into LinkedHashMap
        ships = new LinkedHashMap<>();
        ships.put(lightFighters, 0);
        ships.put(smallCargoShips, 0);
        ships.put(heavyFighters, 0);
        ships.put(destroyers, 0);
        ships.put(tanks, 0);
        ships.put(hijackers, 0);
        ships.put(largeCargoShips, 0);
        ships.put(missileLaunchers, 0);
        ships.put(battleships, 0);
    }

    public static Defense missileCannon = new Defense("Missile Cannon", 2000, 1000, 900, 400);
    public static Defense laserCannon = new Defense("Laser Cannon", 3000, 2000, 1500, 900);
    public static Defense plasmaCannon = new Defense("Plasma Cannon", 5000, 4000, 2800, 1900);
    public static Defense particleAccelerator = new Defense("Particle Accelerator", 10000, 8000, 5600, 4100);
    static {
        missileCannon      .setLevelRequired(1); missileCannon      .setPower(1  );
        laserCannon        .setLevelRequired(2); laserCannon        .setPower(1.5);
        plasmaCannon       .setLevelRequired(3); plasmaCannon       .setPower(3  );
        particleAccelerator.setLevelRequired(5); particleAccelerator.setPower(6.5);
        
        // TODO: Set in constructor instead of static block: make variables final:
        missileCannon.setDescription("Cheap and efficient missile turrets");
        laserCannon.setDescription("Shoots powerful lasers that pierces ships.");
        plasmaCannon.setDescription("Superheated matter can melt through armor easily.");
        particleAccelerator.setDescription("Particle Accelerators blasts concentrated amounts of particles at extreme speeds.");
    }
    private Map<Defense, Integer> defenses;
    private void initializeDefenses() {
        defenses = new LinkedHashMap<>();
        defenses.put(missileCannon, 0);
        defenses.put(laserCannon, 0);
        defenses.put(plasmaCannon, 0);
        defenses.put(particleAccelerator, 0);
    }

    private Building metalMine ;
    private Building crystalMine ;
    private Building deuteriumMine ;
    private Building solarPanel ;
    // private Building researchFacility; TODO: Add researching
    private Building shipyard ;

    // TODO: Make all buildings static to conserve memory and make hashmap of building and level of buildings:
    private void initializeBuildings() {
        metalMine        = new Building("Metal Mine"       , this, 3000, 2250, 250, true);
        crystalMine      = new Building("Crystal Mine"     , this, 4000, 1500, 500, true);
        deuteriumMine    = new Building("Deuterium Mine"   , this, 3500, 2000, 750, true);
        solarPanel       = new Building("Solar Panel"      , this, 3500, 2500, 0   );
        // researchFacility = new Building("Research Facility", this, 6000, 4500, 2000);
        shipyard         = new Building("Shipyard"         , this, 4000, 2000, 0   );
        buildings = Arrays.asList(
            metalMine,
            crystalMine,
            deuteriumMine,
            solarPanel,
            shipyard
        );
    }

    private List<Building> buildings;

    public void executeDefaultActions() { // Executes default actions
        if(unminedMetal > 0) metalMine.mineMetal();
        if(unminedCrystal > 0) crystalMine.mineCrystal();
        if(unminedDeuterium > 0) deuteriumMine.mineDeuterium();
    }

    public List<Building> getBuildings() {
        return buildings;
    }

    // TODO: Stop from repeating 3 times. Maybe make buildings save efficiency stat
    public double enoughEnergy() {
        if(this.empire != Game.player) return 1;
        int energy = solarPanel.getEnergyOutput();
        int energyCost = 0;
        if(unminedMetal > 0) energyCost = energyCost + metalMine.getEnergyCost();
        if(unminedCrystal > 0) energyCost = energyCost + crystalMine.getEnergyCost();
        if(unminedDeuterium > 0) energyCost = energyCost + deuteriumMine.getEnergyCost();
        if(energyCost > energy && energyCost != 0) {
            double efficiency = Math.sqrt((double) energy / energyCost);
            System.out.println("Not enough energy on planet " + this.getName() + "! Output decreased.");
            System.out.println("You need " + (energyCost - energy) + " more energy to reach maximum efficiency.");
            System.out.println("Current efficiency: " + efficiency);
            return efficiency;
        }
        return 1.0;
    } 
    
    public void attackSubtract(int deuteriumCost, Map<Ship, Integer> fleet) {
        this.addDeuterium(deuteriumCost * -1); // subtracts deuterium
        for(Map.Entry<Ship, Integer> entry : fleet.entrySet()) { // Subtracts the fleets from current planet
            this.setShipCount(entry.getKey(), this.getShipCount(entry.getKey()) - entry.getValue());
        }
    }

    public Building getMetalMine() {return metalMine;}
    public int getMetalMineLevel() {return metalMine.getLevel();}
    public Building getCrystalMine() {return crystalMine;}
    public int getCrystalMineLevel() {return crystalMine.getLevel();}
    public Building getDeuteriumMine() {return deuteriumMine;}
    public int getDeuteriumMineLevel() {return deuteriumMine.getLevel();}
    public int getShipyardLevel() {if(this.empire == Game.player) return shipyard.getLevel(); else return 999;}

    public Map<Ship, Integer> getShips() {return ships;}
    public Map<Defense, Integer> getDefenses() {return defenses;}
    public int getShipCount(Ship ship) {return ships.get(ship);}
    public void setShipCount(Ship ship, int number) {ships.put(ship, number);}
    public void setShips(Map<Ship, Integer> ships) {this.ships = ships;}

    public int getDefenseCount(Defense defense) {return defenses.get(defense);}
    public void setDefenseCount(Defense defense, int number) {defenses.put(defense, number);}
    public void setDefenses(Map<Defense, Integer> defenses) {this.defenses = defenses;}

    public String getName() { return name; }
    public String getIndex() { return star.getSystemIndex() + ":" + planetIndex; }
    public String getFullInfo() { return "Planet " + getName() + " at " + stringCoordinates() + " at index " + getIndex(); }
    public String toString() { return getName(); }
    public Empire getEmpire() {return empire;}
    public boolean isColonized() {return colonized;}
    public boolean isDestroyed() {return destroyed;}

    public void addMetal(int diff) {localMetal = localMetal + diff; }
    public int getUnminedMetal() {return unminedMetal;}
    public void remUnminedMetal(int diff) {unminedMetal = unminedMetal - diff;}
    public double getMetalMultiplier() {return metalMultiplier;}
    public void addCrystal(int diff) {localCrystal = localCrystal + diff; }
    public int getUnminedCrystal() {return unminedCrystal;}
    public void remUnminedCrystal(int diff) {unminedCrystal = unminedCrystal - diff;}
    public double getCrystalMultiplier() {return crystalMultiplier;}
    public void addDeuterium(int diff) {localDeuterium = localDeuterium + diff; }
    public int getUnminedDeuterium() {return unminedDeuterium;}
    public void remUnminedDeuterium(int diff) {unminedDeuterium = unminedDeuterium - diff;}
    public double getDeuteriumMultiplier() {return deuteriumMultiplier;}

    public int getMetal() {return localMetal;}
    public int getCrystal() {return localCrystal;}
    public int getDeuterium() {return localDeuterium;}

    public int getMetalGoal() {return metalGoal;}
    public int getCrystalGoal() {return crystalGoal;}
    public int getDeuteriumGoal() {return deuteriumGoal;}
    public void setMetalGoal(int goal) {metalGoal = goal;}
    public void setCrystalGoal(int goal) {crystalGoal = goal;}
    public void setDeuteriumGoal(int goal) {deuteriumGoal = goal;}

}