package bankor;

/**
 * Created by akgunduz on 30/08/15.
 */
public class Address {

    private Address(){}

    private static String getStdString(long address) {

        return Long.toString(address);
    }

    public static Interfaces getInterface(long address) {

        if (address > 0xFFFFFF) {
            return Interfaces.INTERFACE_NET;

        } else if (address > 1000) {
            return Interfaces.INTERFACE_UNIXSOCKET;
        }

        return Interfaces.INTERFACE_PIPE;

    }

    public static String getString(long address) {

        Interfaces _interface = getInterface(address);

        switch (_interface) {

            case INTERFACE_NET:
                return NetAddress.getString(address);

            default:
                return getStdString(address);
        }
    }
}
