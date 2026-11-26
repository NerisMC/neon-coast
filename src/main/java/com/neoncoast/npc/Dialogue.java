package com.neoncoast.npc;

import net.minecraft.util.RandomSource;

/** Names and lines for citizens. All original. */
public final class Dialogue {
    private Dialogue() {
    }

    public static final String[] FIRST_NAMES = {
            "Marcus", "Tasha", "Deshawn", "Maria", "Kevin", "Priya", "Luis", "Jasmine", "Tyrone", "Emily", "Carlos", "Aaliyah",
            "Mike", "Sofia", "Darnell", "Chloe", "Andre", "Nina", "Jake", "Keisha", "Omar", "Brianna", "Tony", "Mei",
            "Ricky", "Destiny", "Hector", "Ashley", "Jamal", "Rosa", "Brandon", "Tiffany", "Vince", "Lena", "Dante", "Kayla"};

    public static final String[] GREET = {
            "Hey, how's it going?", "Nice day in the city, huh?", "You look like you need a coffee.",
            "Watch where you're walking, buddy.", "Don't stand too close, I just bought these shoes.",
            "My landlord raised the rent again. Unbelievable.", "Have you tried the taco truck on 5th? Life changing.",
            "I'm late for work. Again.", "This city never sleeps, and neither do I.", "Traffic was crazy this morning.",
            "You new around here?", "I'm thinking about starting a podcast.", "Can't talk, I'm on a call.",
            "Yo! Love the outfit.", "Rent's due, bills are due, everything's due.", "I swear the pigeons here are getting bigger.",
            "My cousin says he knows a guy who knows a guy.", "If you see my dog, his name is Biscuit.",
            "I've been walking for an hour. Where'd I park?", "Gas prices are criminal, man."};

    public static final String[] DRIVING = {
            "Get off the road!", "Beep beep, coming through!", "You trying to get run over?", "Learn to drive!",
            "I've got somewhere to be!", "Nice car. Mine's nicer."};

    public static final String[] GUN = {
            "Whoa whoa, put that thing away!", "Is that real?! Stay back!", "I don't want any trouble!",
            "Easy now... easy...", "Somebody call the cops!", "Please, I've got a family!"};

    public static final String[] WANTED = {
            "Hey... aren't you the one from the news?", "The cops are looking for you, you know.",
            "I didn't see anything. I never saw you.", "You better lay low for a while."};

    public static final String[] SCARED = {
            "AAAH! Run!", "Gunshots! Everybody down!", "Somebody help!", "Call 911!", "I'm out of here!", "Not again!"};

    public static final String[] HURT = {"Ow! What's your problem?!", "Are you crazy?!", "Help! I'm being attacked!", "Ugh! Leave me alone!"};

    public static final String[] CARJACKED = {"Hey! That's my car!", "Thief! Somebody stop him!", "Not my ride! I just paid it off!",
            "I'm calling the cops!"};

    public static String pick(String[] lines, RandomSource r) {
        return lines[r.nextInt(lines.length)];
    }
}
