package bankor;

/**
 * Created by akgunduz on 30/08/15.
 */
public interface SchedulerCallback {
    boolean onProcess(Address address, Message msg);
}
