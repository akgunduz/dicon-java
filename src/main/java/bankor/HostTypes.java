package bankor;

/**
 * Created by akgunduz on 30/08/15.
 */
public enum HostTypes {

    HOST_DISTRIBUTOR("HOST_DISTRIBUTOR"),
    HOST_COLLECTOR("HOST_COLLECTOR"),
    HOST_NODE("HOST_NODE");

    String name;

    HostTypes(String name) {
        this.name = name;
    }

    int getId() {
        return ordinal();
    }

    String getName() {
        return name;
    }

    public static HostTypes getHost(int id) {
        return values()[id];
    }

    public static String getName(int id) {
        return getHost(id).name;
    }

    public static int getMax() {
        return values().length;
    }
}
