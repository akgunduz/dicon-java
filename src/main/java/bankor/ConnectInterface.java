package bankor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by akgunduz on 10/11/15.
 */
public class ConnectInterface {

    public String name;
    public Interfaces type;

    public long address;
    public long helper;

    private static List<ConnectInterface> interfaceList;

    public ConnectInterface(String name, long address, Interfaces type) {
        this.name = name;
        this.address = address;
        this.helper = 0;
        this.type = type;
    }

    public ConnectInterface(String name, long address, long helper) {
        this.name = name;
        this.address = address;
        this.helper = helper;
        this.type = Interfaces.INTERFACE_NET;
    }

    public static List<ConnectInterface> getInterfaces() {

        if (interfaceList != null) {
            return interfaceList;
        }

        interfaceList = new ArrayList<>();

        interfaceList.addAll(Net.getInterfaces());
//TODO for unix socket and pipe
        return interfaceList;

    }

    public int getCount() {

        return interfaceList.size();

    }

    public static ConnectInterface getInterface(int index) {

        return interfaceList.get(index);

    }

    public static String getName(int index) {

        ConnectInterface ci = interfaceList.get(index);

        return ci.name;
    }

    public static Interfaces getType(int index) {

        ConnectInterface ci = interfaceList.get(index);

        return ci.type;

    }

    public static long getAddress(int index) {

        ConnectInterface ci = interfaceList.get(index);

        return ci.address;
    }

    public static long getHelper(int index) {

        ConnectInterface ci = interfaceList.get(index);

        return ci.helper;
    }
}
