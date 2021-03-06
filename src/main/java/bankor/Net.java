package bankor;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * Created by akgunduz on 30/08/15.
 */
public class Net extends Interface {

    private static final String LOGTAG = "Net";

    private static int portOffset = 0;

    private ServerSocketChannel server;

    private static List<ConnectInterface> interfaceList = null;

    public Net(Unit host, int index, final InterfaceCallback cb, String rootPath) {

        super(host, Interfaces.INTERFACE_NET, index, cb, rootPath);

    }

    @Override
    boolean init(int index) {

        int tryCount = 0;

        try {

            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.socket().setReuseAddress(true);

        } catch (Exception e) {
            System.out.println("Net.init -> " + e.getMessage());
            return false;
        }

        do {

            boolean status = false;

            tryCount++;

            setAddress(index);

            try {

                server.socket().bind(new InetSocketAddress(NetAddress.getInetAddress(address), NetAddress.getPort(address)));
                System.out.println("Socket opened at ip :" + NetAddress.getIPstr(address) +
                        " port : " + NetAddress.getPort(address));

                server.register(selector, SelectionKey.OP_ACCEPT);

                status = true;

            } catch (Exception e) {
                System.out.println("Net.init -> " + e.getMessage());
                if (tryCount == 10) {
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
    void setAddress(int index) {

        address = NetAddress.parseAddress(ConnectInterface.getAddress(index),
                NetAddress.DEFAULT_PORT + portOffset++,
                (int)ConnectInterface.getHelper(index));

    }

    @Override
    void onReceive(Unit host) {

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

                                Message msg = new Message(host, getRootPath());

                                try {

                                    if (msg.readFromStream(client)) {
                                        push(MessageDirection.MESSAGE_RECEIVE, msg.getOwnerAddress(), msg);

                                    } else {
                                        client.finishConnect();
                                    }

                                } catch (Exception e) {
                                    System.out.println("Net.onReceive -> " + e.getMessage());
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
                System.out.println("Net.onReceive -> " + e.getMessage());
                return;
            }
        }
    }


    @Override
    void onSend(long target, Message msg) {

        SocketChannel socket = null;

        try {

            socket = SocketChannel.open();

            InetSocketAddress targetSocket = new InetSocketAddress(NetAddress.getInetAddress(target),
                    NetAddress.getPort(target));

            socket.connect(targetSocket);

            msg.setOwnerAddress(getAddress());
            msg.writeToStream(socket);

        } catch (Exception e) {
        //    System.out.println("Net.runSender -> " + e.getMessage());

        } finally {

            try {

                if (socket != null) {
                    socket.close();
                }

            } catch (Exception e) {
                System.out.println("Net.onSend -> " + e.getMessage());
            }
        }
    }

    public static List<ConnectInterface> getInterfaces() {

        if (interfaceList != null) {
            return interfaceList;
        }

        interfaceList = new ArrayList<>();

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            for (NetworkInterface netInterface : Collections.list(interfaces)) {
                String name = netInterface.getName();
                if (name.startsWith("et") ||
                        name.startsWith("en") ||
                        name.startsWith("br") ||
                        name.startsWith("lo")) {


                    List<InterfaceAddress> addresses = netInterface.getInterfaceAddresses();

                    for (InterfaceAddress address : addresses) {
                        InetAddress inetAddress = address.getAddress();
                        if (inetAddress != null && inetAddress instanceof Inet4Address) {
                            interfaceList.add(new ConnectInterface(name,
                                    NetAddress.getIP(inetAddress.getAddress()), address.getNetworkPrefixLength()));
                            break;
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("NetAddress.getInterfaces-> " + e.getMessage());
            return null;
        }

        return interfaceList;

    }

    public void end() {

        super.end();
        try {

            server.socket().close();

        } catch (Exception e) {
            System.out.println("Net.end -> " + e.getMessage());
        }

    }

    @Override
    List<Long> getAddressList() {

        return NetAddress.getAddressList(address);
    }
}

