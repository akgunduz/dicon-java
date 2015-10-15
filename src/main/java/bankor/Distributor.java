package bankor;

import java.util.ArrayDeque;
import java.util.List;

/**
 * Created by akgunduz on 10/9/15.
 */
public class Distributor extends Component implements NodeCallback {

    public static final String DISTRIBUTOR_PATH = "Distributor";

    ArrayDeque<Long> collectorWaitingList = new ArrayDeque<>();

    DiffTime collStartTime = new DiffTime();

    NodeManager nodeManager;

    public Distributor(int collectorIndex, int nodeIndex, String rootPath, double backupRate) {
        super(generateIndex(0xFFFF, collectorIndex, nodeIndex), rootPath);

        nodeManager = new NodeManager(this, backupRate);

        UI.DIST_ADDRESS.update(connectors[HostTypes.HOST_COLLECTOR.getId()].getAddress(),
                connectors[HostTypes.HOST_NODE.getId()].getAddress());
    }

    @Override
    public boolean processDistributorMsg(long address, Message msg) {
        return false;
    }

    @Override
    public boolean processCollectorMsg(long address, Message msg) {

        boolean status = false;

        switch(msg.getType()) {

            case MSGTYPE_READY:

                UI.DIST_COLL_LIST.update(address, 0L);
                UI.DIST_LOG.update("\"READY\" msg from collector: " + Address.getString(address));
                break;

            case MSGTYPE_NODE:

                UI.DIST_LOG.update("\"NODE\" msg from collector: " + Address.getString(address));
                status = send2CollectorMsg(address, MessageTypes.MSGTYPE_NODE);
                break;

            case MSGTYPE_TIME:

                collStartTime.start();
                nodeManager.resetTimes();
                UI.DIST_LOG.update("\"TIME\" msg from collector: " + Address.getString(address));
                break;

            default:
                break;
        }

        return status;

    }

    @Override
    public boolean processNodeMsg(long address, Message msg) {

        boolean status = false;

        switch(msg.getType()) {

            case MSGTYPE_READY:

                UI.DIST_NODE_LIST.update(address, NodeStates.IDLE);
                UI.DIST_LOG.update("\"READY\" msg from node: " + Address.getString(address));

                nodeManager.setIdle(address, collStartTime.stop());

                if (collectorWaitingList.size() > 0) {

                    long collectorAddress = collectorWaitingList.poll();
                    UI.DIST_LOG.update("Processing a collector from the waiting list: " +
                            Address.getString(collectorAddress));

                    status = send2CollectorMsg(collectorAddress, MessageTypes.MSGTYPE_NODE);

                } else {
                    status = true;
                }

                break;

            case MSGTYPE_ALIVE:

                UI.DIST_LOG.update("\"ALIVE\" msg from node: " + Address.getString(address));

                if (!nodeManager.validate(address)
                        && collectorWaitingList.size() > 0) {

                    long collectorAddress = collectorWaitingList.poll();
                    UI.DIST_LOG.update("Processing a collector from the waiting list: " +
                            Address.getString(collectorAddress));

                    status = send2CollectorMsg(collectorAddress, MessageTypes.MSGTYPE_NODE);

                } else {
                    status = true;
                }

                UI.DIST_NODE_LIST.update(address, NodeStates.IDLE);

                break;

            case MSGTYPE_BUSY:

                UI.DIST_LOG.update("\"BUSY\" msg from node: " + Address.getString(address));

                nodeManager.setBusy(address);
                UI.DIST_NODE_LIST.update(address, NodeStates.BUSY);

                status = true;
                break;

            case MSGTYPE_TIMEOUT:

                UI.DIST_LOG.update("\"TIMEOUT\" msg from node: " + Address.getString(address));

                nodeManager.remove(address);
                UI.DIST_NODE_LIST.update(address, NodeStates.REMOVE);

                status = send2CollectorMsg(msg.getVariant(0), MessageTypes.MSGTYPE_NODE);

                break;

            default :
                break;
        }

        return status;

    }

    boolean send2NodeMsg(long address, MessageTypes type) {

        Message msg = new Message(HostTypes.HOST_DISTRIBUTOR, type, getRootPath());

        switch(type) {

            case MSGTYPE_WAKEUP:

                UI.DIST_LOG.update("\"WAKEUP\" msg sent to node: " + Address.getString(address));
                break;

            default:
                return false;

        }

        return connectors[HostTypes.HOST_NODE.getId()].send(address, msg);

    }

    boolean send2CollectorMsg(long address, MessageTypes type) {

        Message msg = new Message(HostTypes.HOST_DISTRIBUTOR, type, getRootPath());

        switch(type) {

            case MSGTYPE_WAKEUP:

                UI.DIST_LOG.update("\"WAKEUP\" msg sent to collector: " + Address.getString(address));
                break;

            case MSGTYPE_NODE:

                long nodeAddress = nodeManager.getIdle(address);

                if (nodeAddress != 0) {

                    UI.DIST_NODE_LIST.update(nodeAddress, NodeStates.PREBUSY);
                    UI.DIST_COLL_LIST.update(address, nodeAddress);
                    UI.DIST_LOG.update("\"NODE\" msg sent to collector: " + Address.getString(address) +
                        " with available node: " + Address.getString(nodeAddress));

                    msg.setVariant(0, nodeAddress);

                } else {

                    collectorWaitingList.offer(address);

                    UI.DIST_COLL_LIST.update(address, 0L);
                    UI.DIST_LOG.update("\"NODE\" msg sent to collector: " + Address.getString(address) +
                            " with no available node");

                    msg.setVariant(0, 0);
                }

                break;

            default:
                return false;
        }

        return connectors[HostTypes.HOST_COLLECTOR.getId()].send(address, msg);

    }

    boolean sendWakeupMessage(int index) {

        List<Long> list = connectors[index].getAddressList();

        for (Long item : list) {

            Message msg = new Message(HostTypes.HOST_DISTRIBUTOR, MessageTypes.MSGTYPE_WAKEUP, getRootPath());
            connectors[index].send(item, msg);

        }

        UI.DIST_LOG.update("\"WAKEUP\" messages sent to network");

        return true;
    }

    boolean sendWakeupMessagesAll() {

        sendWakeupMessage(HostTypes.HOST_COLLECTOR.getId());
        if (connectors[HostTypes.HOST_COLLECTOR.getId()] != connectors[HostTypes.HOST_NODE.getId()]) {
            sendWakeupMessage(HostTypes.HOST_NODE.getId());
        }
        return true;
    }

    boolean reset() {

        nodeManager.clear();
        collectorWaitingList.clear();
        collStartTime.reset();
        return true;

    }

    @Override
    public boolean onTimeOut(NodeItem node) {

        Message msg = new Message(HostTypes.HOST_NODE, MessageTypes.MSGTYPE_TIMEOUT, getRootPath());
        msg.setVariant(0, node.lastServedCollector);
        connectors[HostTypes.HOST_NODE.getId()].put(node.address, msg);

        return true;
    }

    @Override
    public boolean onWakeup() {

        return sendWakeupMessage(HostTypes.HOST_NODE.getId());

    }
}
