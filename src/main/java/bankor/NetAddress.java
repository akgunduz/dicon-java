package bankor;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by akgunduz on 30/08/15.
 */
public class NetAddress {

    private static final long IPADDRESS_MASK = 0xFFFFFFFFL;
    private static final long LOOPBACK_ADDRESS = 0x0100007FL;
    private static final int LOOPBACK_RANGE = 256;
    public static final int DEFAULT_PORT = 61001;

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

    public static long parseAddress(long ip, int port, int netmask) {

        return ((long)netmask << 48) | ((long)port << 32) | ip;
    }

    public static long getIP(long address) {

        return address & IPADDRESS_MASK;
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

        return (int)((address >> 32) & 0xFFFF);
    }

    public static int getNetmask(long address) {

        return (int)((address >> 48) & 0xFFFF);
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

    public static List<Long> getAddressList(long address) {

        List<Long> list = new ArrayList<>();

        int netmask = getNetmask(address);

        long ownIP = getIP(address);

        if (NetAddress.isLoopback(address)) {

            for (int i = 0; i < LOOPBACK_RANGE; i++) {

                long destAddress = parseAddress(ownIP, DEFAULT_PORT + i, netmask);

                if (destAddress != address) {

                    list.add(destAddress);

                }

            }

        } else {

            long range = 1 << (32 - netmask);

            long startIP = (1 >> netmask) & ownIP + 1;

            for (int i = 0; i < range; i++) {

                if (startIP != ownIP) {

                    long destAddress = parseAddress(startIP, DEFAULT_PORT, netmask);

                    list.add(destAddress);

                }

                startIP++;
            }

        }

        return list;
    }
}

