package bankor;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by akgunduz on 27-Aug-15.
 */
public class NodeWatchdog {

    private static final int NODE_TIMEOUT = 10000;
    private static final int WAKEUP_TIMEOUT = 30000;

    private Timer timer;

    public NodeWatchdog(final NodeCallback nodeCallback) {

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {

                nodeCallback.onWakeup();
            }

        }, 0, WAKEUP_TIMEOUT);

    }

    public NodeWatchdog(final NodeItem node, final NodeCallback nodeCallback) {

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {

                nodeCallback.onTimeOut(node);
            }

        }, NODE_TIMEOUT);

    }

    public void end() {
        timer.cancel();
    }
}
