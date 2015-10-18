package bankor;

/**
 * Created by akgunduz on 30/08/15.
 */
public enum ArchTypes {

    ARCH_OSX("osx"),
    ARCH_WIN("win"),
    ARCH_LIN("lin"),
    ARCH_ARM11("arm11"),
    ARCH_A7("a7"),
    ARCH_A8("a8"),
    ARCH_A9("a9"),
    ARCH_A15("a15");

    String dir;

    ArchTypes(String dir) {
        this.dir = dir;
    }

    int getId() {
        return ordinal();
    }

    String getDir() {
        return dir;
    }

    public static ArchTypes getArch(int id) {
        return values()[id];
    }

    public static String getDir(int id) {
        return getArch(id).dir;
    }

    public static int getMax() {
        return values().length;
    }
}
