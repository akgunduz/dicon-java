package bankor;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by akgunduz on 30/08/15.
 */
public class Scheduler implements Runnable {

    private static final String LOGTAG = "Scheduler";

    public static final int MAX_SCHEDULER_CAPACITY = 100;

    final ArrayBlockingQueue<Capsule> messages;

    int capacity;

    final InterfaceCallback[] mCB;

    class Capsule {
        MessageDirection type;
        long address;
        Message msg;

        Capsule(MessageDirection type, long address, Message msg) {
            this.type = type;
            this.address = address;
            this.msg = msg;
        }
    }

    public Scheduler(int capacity) {

        this.capacity = capacity;

        messages = new ArrayBlockingQueue<>(1000);
        mCB = new InterfaceCallback[MessageDirection.values().length];

        new Thread(this).start();
    }

    @Override
    public void run() {

        while(true) {

            try {

                Capsule capsule = messages.take();
                if (capsule.type == MessageDirection.MESSAGE_END) {
                    return;
                } else {
                    mCB[capsule.type.ordinal()].cb.onProcess(capsule.address, capsule.msg);
                }

            } catch (Exception e) {
                System.out.println("Scheduler.run -> " + e.getCause().getMessage());
                return;
            }

        }
    }

    public boolean push(MessageDirection type, long target, Message msg) {

        try {

            Capsule capsule = new Capsule(type, target, msg);
            messages.put(capsule);

        } catch (Exception e) {
            System.out.println("Scheduler.push -> " + e.getMessage());
            return false;
        }

        return true;
    }

    public void end() {

        push(MessageDirection.MESSAGE_END, 0, null);

    }

    public void setReceiveCB(final InterfaceCallback interfaceCallback) {

        mCB[MessageDirection.MESSAGE_RECEIVE.ordinal()] = interfaceCallback;

    }

    public void setSendCB(int interfaceID, final InterfaceCallback interfaceCallback) {

        mCB[MessageDirection.MESSAGE_SEND.ordinal() + interfaceID] = interfaceCallback;

    }
}
