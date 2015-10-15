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

        App.getInstance().updateUI(0, Address.getString(connectors[HostTypes.HOST_COLLECTOR.getId()].getAddress()),
                Address.getString(connectors[HostTypes.HOST_NODE.getId()].getAddress()));
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
                //LOG_U(UI_UPDATE_DIST_COLL_LIST, address, (uint64_t)0L);
                //LOG_U(UI_UPDATE_DIST_LOG,
               //         "\"READY\" msg from collector: %s", Address::getString(address).c_str());
                break;

            case MSGTYPE_NODE:
              //  LOG_U(UI_UPDATE_DIST_LOG,
             //           "\"CLIENT\" msg from collector: %s", Address::getString(address).c_str());

                status = send2CollectorMsg(address, MessageTypes.MSGTYPE_NODE);
                break;

            case MSGTYPE_TIME:
                collStartTime.start();
                nodeManager.resetTimes();
            //    LOG_U(UI_UPDATE_DIST_LOG,
            //            "\"TIME\" msg from collector: %s", Address::getString(address).c_str());
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
            //    LOG_U(UI_UPDATE_DIST_LOG,
            //            "\"READY\" msg from client: %s", Address::getString(address).c_str());

           //     LOG_U(UI_UPDATE_DIST_CLIENT_LIST, address, IDLE);

                nodeManager.setIdle(address, collStartTime.stop());

                if (collectorWaitingList.size() > 0) {

                    long collectorAddress = collectorWaitingList.poll();

         //           LOG_U(UI_UPDATE_DIST_LOG,
         //                   "Processing a collector from the waiting list: %s",
        //                    Address::getString(collectorAddress).c_str());

                    status = send2CollectorMsg(collectorAddress, MessageTypes.MSGTYPE_NODE);

                } else {
                    status = true;
                }

                break;

            case MSGTYPE_ALIVE:
           //     LOG_U(UI_UPDATE_DIST_LOG,
             //           "\"ALIVE\" msg from client: %s", Address::getString(address).c_str());

                if (!nodeManager.validate(address)
                        && collectorWaitingList.size() > 0) {

                    long collectorAddress = collectorWaitingList.poll();

          //          LOG_U(UI_UPDATE_DIST_LOG,
          //                  "Processing a collector from the waiting list: %s", Address::getString(collectorAddress).c_str());

                    status = send2CollectorMsg(collectorAddress, MessageTypes.MSGTYPE_NODE);

                } else {
                    status = true;
                }

         //       LOG_U(UI_UPDATE_DIST_CLIENT_LIST, address, IDLE);

                break;

            case MSGTYPE_BUSY:
         //       LOG_U(UI_UPDATE_DIST_LOG,
         //               "\"BUSY\" msg from client: %s", Address::getString(address).c_str());

                nodeManager.setBusy(address);
                //          LOG_U(UI_UPDATE_DIST_CLIENT_LIST, address, BUSY);


                status = true;
                break;

            case MSGTYPE_TIMEOUT:
       //         LOG_U(UI_UPDATE_DIST_LOG,
       //                 "\"TIMEOUT\" msg from client: %s", Address::getString(address).c_str());

                nodeManager.remove(address);
                //         LOG_U(UI_UPDATE_DIST_CLIENT_LIST, address, REMOVE);

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

          //      LOG_U(UI_UPDATE_DIST_LOG,
         //               "\"WAKEUP\" msg sent to client: %s", Address::getString(address).c_str());
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

             //   LOG_U(UI_UPDATE_DIST_LOG,
            //            "\"WAKEUP\" msg sent to collector: %s", Address::getString(address).c_str());
                break;

            case MSGTYPE_NODE:

                long nodeAddress = nodeManager.getIdle(address);

                if (nodeAddress != 0) {

               //     LOG_U(UI_UPDATE_DIST_CLIENT_LIST, clientAddress, PREBUSY);
               //     LOG_U(UI_UPDATE_DIST_LOG,
              //              "\"CLIENT\" msg sent to collector: %s with available client: %s",
              //              Address::getString(address).c_str(),
              //              Address::getString(clientAddress).c_str());

                    msg.setVariant(0, nodeAddress);

                    //   LOG_U(UI_UPDATE_DIST_COLL_LIST, address, clientAddress);

                } else {

                    collectorWaitingList.offer(address);

            //        LOG_U(UI_UPDATE_DIST_LOG,
             //               "\"CLIENT\" msg sent to collector: %s with no available client",
             //               Address::getString(address).c_str());

                    msg.setVariant(0, 0);

            //        LOG_U(UI_UPDATE_DIST_COLL_LIST, address, (uint64_t)0L);
                }

                break;

            default:
                return false;
        }

        return connectors[HostTypes.HOST_COLLECTOR.getId()].send(address, msg);

    }

    boolean sendWakeupMessage(int index) {

        List<Long> list = connectors[index].getAddressList();

        for (int i = 0; i < 5; /*list.size();*/ i++) {

            Message msg = new Message(HostTypes.HOST_DISTRIBUTOR, MessageTypes.MSGTYPE_WAKEUP, getRootPath());
            connectors[index].send(list.get(i), msg);

        }

        //      LOG_U(UI_UPDATE_DIST_LOG,
        //              "\"WAKEUP\" messages sent to network");

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
