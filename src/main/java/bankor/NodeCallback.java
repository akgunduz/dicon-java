package bankor;

/**
 * Created by akgunduz on 10/11/15.
 */
public interface NodeCallback {

    boolean onTimeOut(NodeItem node);
    boolean onWakeup();
}
