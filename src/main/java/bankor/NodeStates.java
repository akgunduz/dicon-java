package bankor;

/**
 * Created by akgunduz on 30/08/15.
 */
public enum NodeStates {
    IDLE("IDLE"),
    BUSY("BUSY"),
    PREBUSY("PREBUSY"),
    REMOVE("REMOVE");

    String name;

    NodeStates(String name) {
        this.name = name;
    }

    int getId() {
        return ordinal();
    }

    String getName() {
        return name;
    }

    public static NodeStates getState(int id) {
        return values()[id];
    }

    public static String getName(int id) {
        return getState(id).name;
    }

    public static int getMax() {
        return values().length;
    }
}
