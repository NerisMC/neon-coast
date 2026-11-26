package com.pipsparty.entity;

/**
 * The Pip's Party Palace band (all original characters).
 *
 * @param speed      walking speed attribute
 * @param huntSpeed  multiplier when chasing at night
 * @param door       which office door they come to in a shift: 0 = left (west), 1 = right (south), -1 = either
 */
public enum Character {
    PIP("Pip the Raccoon", "the bandleader. Only moves when you're not looking.", 0.24, 1.0, -1),
    RUSTY("Rusty the Robot Dog", "plays guitar. Wanders the west side.", 0.26, 1.05, 0),
    MARLA("Marla the Mole", "plays keytar. Creeps up the east hall.", 0.25, 1.0, 1),
    GNASH("Gnash the Gator", "hides in Gator Grotto. Check on him, or he RUNS.", 0.27, 1.9, 0);

    public final String displayName;
    public final String blurb;
    public final double speed;
    public final double huntSpeed;
    public final int door;

    Character(String displayName, String blurb, double speed, double huntSpeed, int door) {
        this.displayName = displayName;
        this.blurb = blurb;
        this.speed = speed;
        this.huntSpeed = huntSpeed;
        this.door = door;
    }

    public String shortName() {
        return displayName.split(" ")[0];
    }

    /** How aggressive this character is on a given night (0-20, rolled against a d20 every move opportunity). */
    public int aiLevel(int night, int hour) {
        int[][] table = {
                // night: 1  2  3  4   5
                {0, 1, 3, 6, 10},   // PIP (and +2 after 3 AM)
                {2, 4, 7, 10, 14},  // RUSTY
                {1, 4, 7, 11, 15},  // MARLA
                {0, 3, 6, 9, 13},   // GNASH
        };
        int n = Math.max(1, Math.min(5, night)) - 1;
        int level = table[ordinal()][n] + (hour >= 3 ? 2 : 0) + (night > 5 ? night - 5 : 0);
        return Math.min(20, level);
    }
}
