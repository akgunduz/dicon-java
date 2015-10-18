package bankor;

import java.util.List;

/**
 * Created by akgunduz on 30/08/15.
 */
public class Connector {

    Interface _interface;

    public Connector(Unit host, int index, InterfaceCallback callback, String rootPath) {

        _interface = new Net(host, index, callback, rootPath);

    }

    public boolean send(long target, Message msg) {

        return _interface.push(MessageDirection.MESSAGE_SEND, target, msg);
    }

    public boolean put(long target, Message msg) {

        return _interface.push(MessageDirection.MESSAGE_RECEIVE, target, msg);
    }

    public long getAddress() {

        return _interface.getAddress();
    }

    public List<Long> getAddressList() {

        return _interface.getAddressList();
    }

    public Interfaces getInterfaceType() {

        return _interface.getType();
    }

    public Interface getInterface() {

        return _interface;
    }

    public void end() {

        _interface.end();
    }
}
