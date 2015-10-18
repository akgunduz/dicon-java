package bankor;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by akgunduz on 10/11/15.
 */
public class NodeManager {

    private Map<Long, NodeItem> nodes = new HashMap<>();

    private NodeCallback nodeCallback;

    private double backupRate;
    private int readyBackup = 0;
    private int totalBackup = 0;

    private NodeWatchdog nodeWatchdog = null;

    public NodeManager(NodeCallback nodeCallback, double backupRate) {

        this.nodeCallback = nodeCallback;
        this.backupRate = backupRate;
        if (nodeWatchdog == null) {
            nodeWatchdog = new NodeWatchdog(nodeCallback);
        }
    }

    public NodeItem getIdle(long collectorAddress) {

        NodeItem leastUsedNode = null;

        int idleCount = 0;

        for (Map.Entry<Long, NodeItem> entry : nodes.entrySet()) {

            NodeItem node = entry.getValue();
            if (node.state != NodeStates.IDLE) {
                continue;
            }

            idleCount++;

            if (leastUsedNode == null) {
                leastUsedNode = node;
                continue;
            }

            if (node.usage < leastUsedNode.usage) {
                leastUsedNode = node;
            }
        }

        if (leastUsedNode != null && idleCount - totalBackup + readyBackup > 0) {
            leastUsedNode.state = NodeStates.PREBUSY;
            leastUsedNode.lastServedCollector = collectorAddress;

            if (leastUsedNode.watchdog != null) {
                leastUsedNode.watchdog.end();
            }
            leastUsedNode.watchdog = new NodeWatchdog(leastUsedNode, nodeCallback);

        //    LOG_T("Client at address : %s returned to collector",
        //            Address::getString(leastUsedClient->address).c_str());

            return leastUsedNode;

        } else {

        //    LOG_W("No available client right now.");
        }

        return null;
    }

    boolean setIdle(long address, short id, double totalTime) {

        NodeItem node = nodes.get(address);

        if (node != null) {
            node.state = NodeStates.IDLE;
            // LOG_T("Client at address : %s switch to state : %s", Address::getString(address).c_str(), sStates[IDLE]);

            if (node.stopWatch.isInitiated()) {

                //     LOG_U(UI_UPDATE_DIST_LOG,
                //              "Client at address : %s finished job in %.3lf seconds, total time passed : %.3lf",
                //               Address::getString(address).c_str(), clientMap->diffTime.stop(), totalTime);
            }

            return true;

        } else {

            add(address, id);
            return false;
        }
    }

    boolean validate(long address, short id) {

        NodeItem node = nodes.get(address);
        if (node == null) {
            add(address, id);
            return false;
        }

       // LOG_T("Client at address : %s is Alive", Address::getString(address).c_str());
        return true;

    }

    boolean setBusy(long address) {

        NodeItem node = nodes.get(address);
        if (node != null) {
            if (node.watchdog != null) {
                node.watchdog.end();
            }
            node.state = NodeStates.BUSY;
            node.usage++;
            node.stopWatch.start();
            //LOG_T("Client at address : %s switch to state : %s", Address::getString(address).c_str(), sStates[BUSY]);
            return true;
        } else {
           // LOG_E("Could not found a client with address : %s", Address::getString(address).c_str());
            return false;
        }
    }

    boolean remove(long address) {

        NodeItem node = nodes.remove(address);
        if (node != null) {
            readyBackup = Math.min(totalBackup, readyBackup + 1);
         //   LOG_U(UI_UPDATE_DIST_BACKUP, totalBackup, readyBackup);
         //   LOG_T("Client at address %s removed from the list", Address::getString (address).c_str());
            return true;

        } else {
       //     LOG_E("Could not found a client with address : %s", Address::getString(address).c_str());
            return false;
        }
    }

    boolean add(long address, short id) {

        nodes.put(address, new NodeItem(NodeStates.IDLE, 0, address, id));

        totalBackup = nodes.size() == 1 ? 0 : (int)Math.ceil(nodes.size() * backupRate);

        readyBackup = 0;

     //   LOG_U(UI_UPDATE_DIST_BACKUP, totalBackup, readyBackup);
     //   LOG_T("Client at address : %s added to the list", Address::getString(address).c_str());

        return true;
    }

    boolean resetTimes() {

        for (Map.Entry<Long, NodeItem> entry : nodes.entrySet()) {

            NodeItem node = entry.getValue();
            node.stopWatch.reset();
        }

        return true;
    }

    void clear() {

        nodes.clear();
        readyBackup = 0;
        totalBackup = 0;
    }

    void end() {

        if (nodeWatchdog != null) {
            nodeWatchdog.end();
        }

        for (Map.Entry<Long, NodeItem> entry : nodes.entrySet()) {

            NodeItem node = entry.getValue();
            node.end();
        }
    }
}
