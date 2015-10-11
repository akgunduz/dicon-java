package bankor;

import java.net.InetAddress;

/**
 * Created by akgunduz on 30/08/15.
 */
public class NetAddress {

    private static final long IPADDRESS_MASK = 0xFFFFFFFFL;
    private static final long LOOPBACK_ADDRESS = 0x7F000001L;

    private NetAddress(){}

    public static String getString(long address) {

        return getIPstr(address) + ":" + getPort(address);
    }

    private static long parseIP(String ip) {

        long newIP = 0;

        try {

            InetAddress inetAddress =  InetAddress.getByName(ip);

            byte[] pieces = inetAddress.getAddress();

            for (int i = 0; i < 4; i++) {
                newIP |= ((long)pieces[i]) << (8 * i);
            }

        } catch (Exception e) {
            System.out.println("NetAddress.NetAddress-> " + e.getMessage());
        }

        return newIP;
    }

    public static long parseAddress(long ip, int port) {

        return ((long)port << 40) | ip;
    }

    public static long getIP(long address) {

        return address & 0xFFFFFFFFL;
    }

    public static long getIP(byte[] pieces) {

        long ip = 0;

        for (int i = 0; i < 4; i++) {
            ip |= pieces[i] << (8 * i);
        }

        return ip;
    }

    public static String getIPstr(long address) {

        byte[] pieces = getPieces(getIP(address));
        String sIP = "";

        for (int i = 0; i < 4; i++) {
            sIP += Long.toString(pieces[i]);
            sIP += ".";
        }

        return sIP.substring(0, sIP.length() - 1);
    }

    public static int getPort(long address) {

        return (int)((address >> 40) & 0xFFFF);
    }

    public static InetAddress getInetAddress(long address) {

        byte[] pieces = getPieces(getIP(address));

        try {

            return InetAddress.getByAddress(pieces);

        } catch (Exception e) {
            System.out.println("NetAddress.getInetAddress-> " + e.getMessage());
        }


        return null;
    }

    public static boolean isLoopback(long address) {

        return  (((address) & IPADDRESS_MASK) == LOOPBACK_ADDRESS);
    }

    private static byte[] getPieces(long ip) {

        byte[] pieces = new byte[4];

        for (int i = 0; i < 4; i++) {
            pieces[i] = (byte)((ip >> (8 * i)) & 0xFF);
        }

        return pieces;
    }
}

