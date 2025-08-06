package movieMentor.enums;

public enum TopMoviesData {

    THE_SHAWSHANK_REDEMPTION("The Shawshank Redemption", "Two imprisoned men bond over years, finding solace and eventual redemption."),
    THE_GODFATHER("The Godfather", "The aging patriarch of an organized crime dynasty transfers control to his reluctant son."),
    THE_DARK_KNIGHT("The Dark Knight", "Batman faces the Joker, a criminal mastermind unleashing chaos in Gotham."),
    AVENGERS_ENDGAME("Avengers: Endgame", "The Avengers assemble to undo Thanos’ catastrophic snap and restore balance."),
    TITANIC("Titanic", "A young couple fall in love aboard the ill-fated RMS Titanic."),
    INCEPTION("Inception", "A thief invades dreams to steal and plant ideas deep within the subconscious."),
    INTERSTELLAR("Interstellar", "A group of explorers travel through a wormhole in space to save humanity."),
    DUNE_PART_TWO("Dune: Part Two", "Paul Atreides joins the Fremen and rises to lead a holy war across the galaxy."),
    OPPENHEIMER("Oppenheimer", "The story of J. Robert Oppenheimer and the creation of the atomic bomb."),
    BARBIE("Barbie", "Barbie leaves Barbie Land to discover the real world and confront her identity."),
    SPIDERMAN_NO_WAY_HOME("Spider-Man: No Way Home", "Peter Parker seeks help from Doctor Strange to restore his secret."),
    TOP_GUN_MAVERICK("Top Gun: Maverick", "After decades, Maverick returns to train elite pilots for a dangerous mission."),
    THE_SUPER_MARIO_BROS("The Super Mario Bros. Movie", "Mario and Luigi must save the Mushroom Kingdom from Bowser."),
    AVATAR_2("Avatar: The Way of Water", "Jake Sully and Neytiri protect their family in the oceans of Pandora."),
    GUARDIANS_3("Guardians of the Galaxy Vol. 3", "The Guardians face new challenges while protecting Rocket’s past."),
    THE_BATMAN("The Batman", "Batman investigates a web of corruption and serial killings in Gotham."),
    MISSION_IMPOSSIBLE_7("Mission: Impossible – Dead Reckoning Part One", "Ethan Hunt races to stop a powerful AI from taking over."),
    JOHN_WICK_4("John Wick: Chapter 4", "John Wick faces global assassins while seeking freedom from the High Table."),
    FROZEN_2("Frozen II", "Elsa, Anna, Kristoff, and Olaf set out to discover the origin of Elsa's powers."),
    TOY_STORY_4("Toy Story 4", "Woody and the gang welcome a new toy named Forky."),
    BLACK_PANTHER("Black Panther", "T’Challa returns to Wakanda to claim his throne and fight a new challenger."),
    NO_TIME_TO_DIE("No Time to Die", "James Bond comes out of retirement for one final mission."),
    JOKER("Joker", "A mentally troubled man transforms into the criminal mastermind known as the Joker."),
    THE_LION_KING("The Lion King (2019)", "A young lion prince flees his kingdom and learns the meaning of responsibility."),
    FURIOUS_7("Furious 7", "The team fights to protect one of their own from a vengeful assassin."),
    MINIONS_RISE_OF_GRU("Minions: The Rise of Gru", "Young Gru joins forces with the Minions to become a supervillain."),
    THE_HUNGER_GAMES("The Hunger Games", "A girl volunteers to participate in a televised fight to the death."),
    IT("It (2017)", "A group of kids face an ancient evil terrorizing their town."),
    DEADPOOL("Deadpool", "A former mercenary with accelerated healing hunts down his enemies."),
    CREED_3("Creed III", "Adonis Creed faces his past when an old friend returns as a rival."),
    DOCTOR_STRANGE_2("Doctor Strange in the Multiverse of Madness", "Strange faces alternate realities with Scarlet Witch."),
    THE_CREATOR("The Creator", "A future war erupts between humanity and AI-powered beings."),
    ELEMENTAL("Elemental", "In a city of fire, water, air, and earth residents, opposites attract."),
    SOUND_OF_FREEDOM("Sound of Freedom", "A former agent embarks on a mission to save children from trafficking."),
    THE_MENU("The Menu", "A couple discovers a dark secret during a gourmet dining experience."),
    EVERYTHING_EVERYWHERE("Everything Everywhere All at Once", "An unlikely hero must connect multiverse versions of herself."),
    KILLERS_OF_FLOWER_MOON("Killers of the Flower Moon", "FBI investigates murders in the Osage Nation over oil rights."),
    WONKA("Wonka", "A young Willy Wonka explores magic and chocolate in a prequel story."),
    THE_HOLDOVERS("The Holdovers", "A teacher and a troubled student bond during the holiday break."),
    NAPOLEON("Napoleon", "A grand portrait of the life, ambition, and downfall of Napoleon Bonaparte."),
    TAYLOR_SWIFT_ERAS("Taylor Swift: The Eras Tour", "A concert film capturing Swift’s iconic Eras Tour."),
    FIVE_NIGHTS("Five Nights at Freddy’s", "A night guard at a pizzeria discovers deadly animatronic secrets."),
    ASTEROID_CITY("Asteroid City", "A youth convention in the desert is interrupted by cosmic events."),
    SALTBURN("Saltburn", "A student enters the surreal world of a wealthy friend’s family estate."),
    THE_FLASH("The Flash", "Barry Allen alters the timeline, causing catastrophic consequences."),
    ANT_MAN_3("Ant-Man and the Wasp: Quantumania", "Scott Lang battles Kang in the Quantum Realm."),
    MEG_2("Meg 2: The Trench", "A deep-sea expedition unleashes giant prehistoric sharks."),
    THE_WEDDING("the wedding","wedding");

    private final String title;
    private final String description;

    TopMoviesData(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
