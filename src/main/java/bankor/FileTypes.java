package bankor;

/**
 * Created by akgunduz on 30/08/15.
 */
public enum FileTypes {

    FILE_RULE,
    FILE_COMMON,
    FILE_ARCH;

    int getId() {
        return ordinal();
    }

    public static FileTypes getType(int id) {
        return values()[id];
    }

    public static int getMax() {
        return values().length;
    }
}
