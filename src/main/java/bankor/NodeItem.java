package bankor;

/**
 * Created by akgunduz on 10/11/15.
 */
public class NodeItem {

    public NodeStates state = NodeStates.IDLE;
    public int usage;
    public long address;
    public long lastServedCollector;
    public short id;
    public StopWatch stopWatch = new StopWatch();
    public NodeWatchdog watchdog = null;

    public NodeItem(NodeStates state, int usage, long address, short id) {

        this.state = state;
        this.usage = usage;
        this.address = address;
        this.lastServedCollector = 0;
        this.id = id;
    }

    void end() {

        if (watchdog != null) {
            watchdog.end();
        }
    }
}
