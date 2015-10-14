package bankor;

/**
 * Created by akgunduz on 30/08/15.
 */
public enum Interfaces {
    INTERFACE_NET("TCP"),
    INTERFACE_UNIXSOCKET("UnixSocket"),
    INTERFACE_PIPE("Pipe");

    String name;

    Interfaces(String name) {
        this.name = name;
    }

    String getName() {
        return name;
    }

    public static Interfaces getInterface(int id) {
        return values()[id];
    }

    public static String getName(int id) {
        return getInterface(id).getName();
    }
}
