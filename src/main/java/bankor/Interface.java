package bankor;

import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.List;

/**
 * Created by akgunduz on 30/08/15.
 */
public abstract class Interface implements SchedulerCallback, Runnable {

    private static final String LOGTAG = "Interface";

    protected static final byte SHUTDOWN_NOTIFIER = 'Q';

    boolean initialized = false;
    protected long address;
    Scheduler scheduler;
    Pipe notifier;
    Selector selector;
    String rootPath;

    abstract Interfaces getType();
    abstract List<Long> getAddressList();
    abstract void setAddress(int interfaceIndex);

    abstract boolean init(int index);
    abstract void onReceive();
    abstract void onSend(long address, Message msg);

    public Interface(Interfaces type, int interfaceIndex, final InterfaceCallback receiverCallback, String rootPath) {

        this.rootPath = rootPath;

        scheduler = new Scheduler(Scheduler.MAX_SCHEDULER_CAPACITY);

        InterfaceCallback senderCallback = new InterfaceCallback(this, this);

        try {
            selector = Selector.open();
            notifier = Pipe.open();
            notifier.source().configureBlocking(false);
            notifier.source().register(selector, SelectionKey.OP_READ);

        } catch (Exception e) {
            System.out.println("Interface.Interface -> " + e.getMessage());
            return;
        }

        scheduler.setReceiveCB(receiverCallback);
        scheduler.setSendCB(type.ordinal(), senderCallback);

        if (!init(interfaceIndex)) {
            System.out.println("Interface.Interface -> " + "Instance create failed!!!");
            return;
        }

        new Thread(this).start();
        initialized = true;
    }

    void end() {

        ByteBuffer buffer = ByteBuffer.allocate(1);
        buffer.put(SHUTDOWN_NOTIFIER);
        buffer.rewind();
        try {
            notifier.sink().write(buffer);

        } catch (Exception e) {
            System.out.println("Interface.end -> " + e.getMessage());
        }

        scheduler.end();
    }

    @Override
    public boolean onProcess(final long address, final Message msg) {

     //   class OneShotTask implements Runnable {
    //        public void run() {
                onSend(address, msg);
     //       }
    //    }

    //    new Thread(new OneShotTask()).start();
        return true;
    }

    @Override
    public void run() {

        onReceive();
    }

    boolean push(MessageDirection type, long target, Message msg) {

        Interfaces _interface = Address.getInterface(target);
        if (_interface == getType()) {

            scheduler.push(type, target, msg);
            return true;

        }
        System.out.println("Interface.push -> " + "Interface is not suitable for target :" + target);
        return false;

    }

    long getAddress() {

        if (!initialized) {
            return 0;
        }

        return address;
    }

    String getRootPath() {
        return rootPath;
    }
}

