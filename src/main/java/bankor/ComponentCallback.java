package bankor;

/**
 * Created by akgunduz on 30/08/15.
 */
public interface ComponentCallback {
    boolean processDistributorMsg(long address, Message msg);
    boolean processNodeMsg(long address, Message msg);
    boolean processCollectorMsg(long address, Message msg);
}
