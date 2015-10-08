package bankor;

import java.util.List;

/**
 * Created by akgunduz on 30/08/15.
 */
public abstract class Connector implements SchedulerCallback {

    boolean initialized = false;
    Interface _interface;

    public Connector(int index, String rootPath) {

        _interface = new Net(index, new InterfaceCallback(this, this), rootPath);

        initialized = true;
    }

    boolean send(Address target, Message msg) {

        return _interface.push(MessageDirection.MESSAGE_SEND, target, msg);
    }

    Address getAddress() {

        if (!initialized) {
            return null;
        }

        return _interface.getAddress();
    }

    List<Long> getAddressList() {

        if (!initialized) {
            return null;
        }

        return _interface.getAddressList();
    }

    Interfaces getInterfaceType() {

        if (!initialized) {
            return null;
        }

        return _interface.getType();
    }

    Interface getInterface() {

        if (!initialized) {
            return null;
        }

        return _interface;
    }

    void end() {

        _interface.end();
    }
}
