package bankor;

/**
 * Created by akgunduz on 30/08/15.
 */
class InterfaceCallback {

    SchedulerCallback cb;
    Object argument;
    InterfaceCallback(SchedulerCallback cb, Object argument) {
        this.cb = cb;
        this.argument = argument;
    }
};