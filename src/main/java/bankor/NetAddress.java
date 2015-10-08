package bankor;

import java.net.InetAddress;

/**
 * Created by akgunduz on 30/08/15.
 */
public class NetAddress extends Address {

    long ip;
    int port;

    private InetAddress inetAddress;

    public NetAddress(long address) {

        super(address);

        ip = getIP();
        port = getPort();

        byte[] pieces = getPieces(getIP());

        try {

            inetAddress =  InetAddress.getByAddress(pieces);

        } catch (Exception e) {
            System.out.println("NetAddress.NetAddress-> " + e.getMessage());
        }
    }

    public NetAddress(long ip, int port) {

        this.ip = ip;
        this.port = port;

        address = ((long)port << 40) | ip;

        byte[] pieces = getPieces(ip);

        try {

            inetAddress =  InetAddress.getByAddress(pieces);

        } catch (Exception e) {
            System.out.println("NetAddress.NetAddress-> " + e.getMessage());
        }
    }

    public NetAddress(String ip, int port) {

        long newIP = parseIP(ip);

        address = ((long)port << 40) | newIP;

        this.ip = getIP();
        this.port = getPort();
    }

    @Override
    String getString() {

        return getIPstr() + ":" + getPort();
    }

    private long parseIP(String ip) {

        try {

            inetAddress =  InetAddress.getByName(ip);

        } catch (Exception e) {
            System.out.println("NetAddress.NetAddress-> " + e.getMessage());
        }

        byte[] pieces = inetAddress.getAddress();

        long newIP = 0;

        for (int i = 0; i < 4; i++) {
            newIP |= ((long)pieces[i]) << (8 * i);
        }

        return newIP;
    }

    public long getIP() {

        return address & 0xFFFFFFFFL;
    }

    public void setIP(String ip) {

        address = ((long)getPort() << 40) | parseIP(ip);

        this.ip = getIP();
    }

    public String getIPstr() {

        byte[] pieces = getPieces(getIP());
        String sIP = "";
        for (int i = 0; i < 4; i++) {
            sIP += Long.toString(pieces[i]);
            sIP += ".";
        }
        return sIP.substring(0, sIP.length() - 1);
    }

    public int getPort() {

        return (int)((address >> 40) & 0xFFFF);
    }

    public void setPort(int port) {

        address = ((long)port << 40) | getIP();

        this.port = port;
    }

    public Interfaces getInterface() {

        return Interfaces.INTERFACE_NET;
    }

    public InetAddress getInetAddress() {

        return inetAddress;
    }

    private byte[] getPieces(long ip) {

        byte[] pieces = new byte[4];

        for (int i = 0; i < 4; i++) {
            pieces[i] = (byte)((ip >> (8 * i)) & 0xFF);
        }

        return pieces;
    }
}

