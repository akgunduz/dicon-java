package bankor;

/**
 * Created by akgunduz on 10/11/15.
 */
public class NodeItem {

    public boolean isTimerActive = false;

    public NodeStates state = NodeStates.IDLE;
    public int usage;
    public long address;
    public long lastServedCollector;
    public DiffTime diffTime;
    public NodeWatchdog watchdog = null;

    public NodeItem(NodeStates state, int usage, long address) {

        this.state = state;
        this.usage = usage;
        this.address = address;
        this.lastServedCollector = 0;
    }
}
