public class BlackHole extends Coordinates { // Our galaxy is centered around a Black Hole.

    private Star[] stars = new Star[10]; // Array of 10 suns in this galaxy.

    public BlackHole() {
        setCoordinate(0,0,0);
        initializeStars();
    }

    public void initializeStars() { // Initializes the 10 stars and add to array
        for(int i = 0; i < 10; i++) {
            stars[i] = new Star(this, i);
        }
    }

    public Star[] getStars() { return stars; }
    public Star getStar(int i) { // Takes a parameter from 0 to 9 and then returns the planet at that slot.
        if(i >= 0 && i <= 9) {
            return stars[i];
        } else if(i < 0) {
            System.out.println("ERROR: Parameter " + i + " is out of bounds! Nearest in bound parameter returned (0). (getPlanets())");
            return stars[0];
        } else { // (i > 9)
            System.out.println("ERROR: Parameter " + i + " is out of bounds! Nearest in bound parameter returned (9). (getPlanets())");
            return stars[9];
        }
    }
    public Star getRandomStar() {
        int random = (int) (Math.random() * stars.length);
        return stars[random];
    }

}
