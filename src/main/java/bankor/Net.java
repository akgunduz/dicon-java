package bankor;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by akgunduz on 30/08/15.
 */
public class Net extends Interface {

    private static final String LOGTAG = "Net";

    public static final int DEFAULT_PORT = 61001;

    private static int portOffset = 0;

    private ServerSocketChannel server;

    private String selectedIP;


    public Net(int index, final InterfaceCallback cb, String rootPath) {

        super(Interfaces.INTERFACE_NET, index, cb, rootPath);

        System.out.println("Net.Net -> " + "Instance is created!!!");

    }

    @Override
    boolean init(int index) {

        int trycount = 0;

        try {

            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();

            NetworkInterface networkInterface;
            do {
                networkInterface = netInterfaces.nextElement();
                if (index == 0) { //loopback
                    if (networkInterface.isLoopback()) {
                        break;
                    }
                } else {
                    String name = networkInterface.getName();
                    if (name.equals("en0") || name.equals("eth3")) {
                        break;
                    }
                }

            } while(netInterfaces.hasMoreElements());

            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress inetAddress=addresses.nextElement();
                if (inetAddress != null && inetAddress instanceof Inet4Address) {
                    selectedIP = inetAddress.getHostAddress();
                    break;
                }
            }

            server = ServerSocketChannel.open();
            server.configureBlocking(false);

        } catch (Exception e) {
            System.out.println("Net.init -> " + e.getMessage());
            return false;
        }

        do {

            boolean status = false;

            trycount++;

            setAddress(index);

            try {

                server.socket().bind(new InetSocketAddress(((NetAddress) address).getPort()));
                System.out.println("Socket opened at ip :" + ((NetAddress) address).getIPstr() +
                        " port : " + ((NetAddress) address).getPort());

                server.register(selector, SelectionKey.OP_ACCEPT);

                status = true;

            } catch (Exception e) {
                System.out.println("Net.init -> " + e.getMessage());
                if (trycount == 10) {
                    return false;
                }

            }

            if (!status) {
                continue;
            }

            break;

        } while(true);

        return true;
    }

    @Override
    Interfaces getType() {
        return Interfaces.INTERFACE_NET;
    }

    @Override
    List<Long> getAddressList() {
        return null;
    }

    @Override
    void setAddress(int index) {

        address = new NetAddress(selectedIP, DEFAULT_PORT + portOffset++);

    }

    @Override
    void onReceive() {

        boolean thread_started = true;

        while(thread_started) {

            try {

                selector.select();
                Set readyKeys = selector.selectedKeys();
                Iterator iterator = readyKeys.iterator();

                while (iterator.hasNext()) {

                    SelectionKey key = (SelectionKey) iterator.next();

                    if (key.isAcceptable()) {

                        final SocketChannel client = server.accept();

                        class Acceptor implements Runnable {

                            public void run() {

                                Message msg = new Message(getRootPath());

                                try {

                                    if (msg.readFromStream(client)) {
                                        push(MessageDirection.MESSAGE_RECEIVE, new NetAddress(msg.getOwnerAddress()), msg);

                                    } else {
                                        client.finishConnect();
                                    }

                                } catch (Exception e) {
                                    System.out.println("Net.runReceiver -> " + e.getMessage());
                                }
                            }
                        }

                        new Thread(new Acceptor()).start();

                    } else if (key.isReadable()) {

                            ByteBuffer buffer = ByteBuffer.allocate(1);

                            ((Pipe.SourceChannel) key.channel()).read(buffer);
                            buffer.rewind();
                            if (buffer.get() == SHUTDOWN_NOTIFIER) {
                                thread_started = false;
                            }
                    }

                    iterator.remove();
                }

            } catch (Exception e) {
                System.out.println("Net.runReceiver -> " + e.getMessage());
                return;
            }
        }
    }


    @Override
    void onSend(Address target, Message msg) {

        SocketChannel socket = null;

        try {

            InetAddress serverAddr = ((NetAddress)target).getInetAddress();
            socket = SocketChannel.open();
            socket.connect(new InetSocketAddress(serverAddr, ((NetAddress) target).getPort()));

            msg.setOwnerAddress(getAddress().address);
            msg.writeToStream(socket);

        } catch (Exception e) {
            System.out.println("Net.runSender -> " + e.getMessage());

        } finally {

            try {

                if (socket != null) {
                    socket.close();
                }

            } catch (Exception e) {
                System.out.println("Net.runSender -> " + e.getMessage());
            }
        }
    }

    public static int getInterfaceAddress() {
        return 1;
    }
}

