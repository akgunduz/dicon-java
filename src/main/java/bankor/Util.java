package bankor;

import javafx.application.Application;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by akgunduz on 30/08/15.
 */
public class Util {

    static SimpleDateFormat dateFormat;
    static long delta = 0;
    private static final long OFFSET_1900_TO_1970 = ((365L * 70L) + 17L) * 24L * 60L * 60L;

    public static void init(Application app) {
        dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
        getNetworkTime();
    }

    public static Date getTime() {
        return new Date(System.currentTimeMillis() + delta);
    }

    private static void getNetworkTime() {

        final String ntpServer = "pool.ntp.org";
        byte[] ntpData = new byte[48];
        ntpData[0] = 0x1B; //LeapIndicator = 0 (no warning), VersionNum = 3 (IPv4 only), Mode = 3 (Client Mode)

        try {

            InetAddress[] addresses = InetAddress.getAllByName(ntpServer);

            DatagramPacket packet = new DatagramPacket(ntpData, 48, addresses[0], 123);

            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(3000);
            socket.send(packet);
            socket.receive(packet);

            long utcMS = System.currentTimeMillis();

            socket.close();

            ByteBuffer buffer = ByteBuffer.wrap(ntpData);
            long intPart = buffer.getInt(40) & 0xFFFFFFFFL;
            long fracPart = buffer.getInt(44) & 0xFFFFFFFFL;

            long ntpMS = (intPart - OFFSET_1900_TO_1970) * 1000 + ((fracPart * 1000L) / 0x100000000L);

            delta = ntpMS - utcMS;
            System.out.println("Delta -> " + delta);
        } catch (Exception e) {
            System.out.println("Util.GetNetworkTime -> " + e.getMessage());
        }

    }

    public static String getTimeString() {
        Date dt = new Date(System.currentTimeMillis() + delta);
        return dateFormat.format(dt);
    }

    public static byte[] strToHex(String str) {

        int len = str.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4)
                    + Character.digit(str.charAt(i+1), 16));
        }
        return data;
    }
}
