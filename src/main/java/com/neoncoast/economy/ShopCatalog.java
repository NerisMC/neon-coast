package com.neoncoast.economy;

/** What the shop terminal sells. Shared by the client screen and the server. */
public final class ShopCatalog {
    public static final String[] NAMES = {
            "Sedan (car key)", "Sports Car (car key)", "Body Armor", "Repair Kit", "Food x8",
            "Pistol", "SMG", "Shotgun", "Assault Rifle", "Sniper Rifle",
            "Pistol/SMG Ammo x30", "Rifle Ammo x30", "Shotgun Shells x12",
            "Pay off the cops", "ROB THE STORE"};
    public static final long[] PRICES = {800, 3000, 400, 150, 40, 600, 1800, 2200, 3500, 6000, 60, 120, 80, 1000, 0};
    public static final int PAY_OFF = 13;
    public static final int ROB = 14;

    private ShopCatalog() {
    }
}
