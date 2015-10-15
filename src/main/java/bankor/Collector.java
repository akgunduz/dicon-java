package bankor;

import java.util.*;

/**
 * Created by akgunduz on 10/9/15.
 */
public class Collector extends Component {

    public static final String COLLECTOR_PATH = "Collector";

    long distributorAddress = 0;

    Map<Long, Rule> rules = new HashMap<>();

    public Collector(int distributorIndex, int nodeIndex, String rootPath) {
        super(generateIndex(distributorIndex, 0xFFFF, nodeIndex), rootPath);

        UI.COLL_ADDRESS.update(connectors[HostTypes.HOST_DISTRIBUTOR.getId()].getAddress(),
                connectors[HostTypes.HOST_NODE.getId()].getAddress());

    }

    @Override
    public boolean processDistributorMsg(long address, Message msg) {

        boolean status = false;

        switch(msg.getType()) {

            case MSGTYPE_WAKEUP:

                distributorAddress = address;

                UI.COLL_ATT_DIST_ADDRESS.update(address);
                UI.COLL_LOG.update("\"WAKEUP\" msg from distributor: " + Address.getString(address));

                status = send2DistributorMsg(address, MessageTypes.MSGTYPE_READY);
                break;

            case MSGTYPE_NODE:

                long nodeAddress = msg.getVariant(0);

                if (nodeAddress == 0) {
                    UI.COLL_LOG.update("\"NODE\" msg from distributor: " + Address.getString(address) + " No Available Node!!!");

                    status = false;

                    break;
                }

                Rule rule = new Rule(getRootPath(), Rule.RULE_FILE);
                if (!rule.isValid()) {
                    UI.COLL_LOG.update("Could not create a rule from path : " + getRootPath());
                    return false;
                }

                rules.put(nodeAddress, rule);

                UI.COLL_ATT_NODE_ADDRESS.update(nodeAddress);
                UI.COLL_FILE_LIST.update(rules.get(nodeAddress));
                UI.COLL_EXEC_LIST.update(rules.get(nodeAddress));
                UI.COLL_LOG.update("\"NODE\" msg from distributor: " + Address.getString(address) +
                        " Available Client : " + Address.getString(nodeAddress));

                status = send2NodeMsg(nodeAddress, MessageTypes.MSGTYPE_RULE);
                break;

            default:
                break;
        }

        return status;

    }

    @Override
    public boolean processCollectorMsg(long address, Message msg) {
        return false;
    }

    @Override
    public boolean processNodeMsg(long address, Message msg) {

        boolean status = false;

        switch(msg.getType()) {

            case MSGTYPE_MD5:

                UI.COLL_LOG.update("\"MD5\" msg from node: " + Address.getString(address) +
                        " with MD5 count : " + msg.md5List.size());

                Rule rule = rules.get(address);
                if (rule == null) {
                    UI.COLL_LOG.update("Could not find a rule for node : " + Address.getString(address));
                    break;
                }

                for (int i = 0; i < msg.md5List.size(); i++) {
                    //TODO contentler icinde ara
                    for (int j = 0; j < rule.getContentCount(RuleTypes.RULE_FILES); j++) {
                        FileContent content = (FileContent) rule.getContent(RuleTypes.RULE_FILES, j);
                        content.getMD5().flip();
                        //TODO 64 bit le coz
                        if (content.getMD5().equals(msg.md5List.get(i))) {
                            //TODO bu clientte var, gonderme
                            content.setFlaggedToSent(false);
                            break;
                        }
                    }
                }

                UI.COLL_FILE_LIST.update(rule);

                status = send2NodeMsg(address, MessageTypes.MSGTYPE_BINARY);
                break;

            default:
                break;

        }

        return status;

    }

    boolean send2NodeMsg(long address, MessageTypes type) {

        Message msg = new Message(HostTypes.HOST_COLLECTOR, type, getRootPath());

        Rule rule = rules.get(address);
        if (rule == null) {
            UI.COLL_LOG.update("Could not find a rule for node : " + Address.getString(address));
            return false;
        }

        switch(type) {

            case MSGTYPE_RULE:

                msg.setRule(Message.STREAM_RULE, rule);
                UI.COLL_LOG.update("\"RULE\" msg sent to node: " + Address.getString(address));
                break;

            case MSGTYPE_BINARY:

                msg.setRule(Message.STREAM_BINARY, rule);
                UI.COLL_LOG.update("\"BINARY\" msg sent to node: " + Address.getString(address) +
                    " with file binary count : " + rule.getFlaggedFileCount());
                break;

            default:
                return false;

        }

        return connectors[HostTypes.HOST_NODE.getId()].send(address, msg);

    }

    boolean send2DistributorMsg(long address, MessageTypes type) {

        Message msg = new Message(HostTypes.HOST_COLLECTOR, type, getRootPath());

        switch(type) {

            case MSGTYPE_READY:

                UI.COLL_LOG.update("\"READY\" msg sent to distributor: " + Address.getString(address));
                break;

            case MSGTYPE_NODE:

                UI.COLL_LOG.update("\"NODE\" msg sent to distributor: " + Address.getString(address));
                break;

            case MSGTYPE_TIME:

                UI.COLL_LOG.update("\"TIME\" msg sent to distributor: " + Address.getString(address));
                break;

            default:
                return false;
        }

        return connectors[HostTypes.HOST_DISTRIBUTOR.getId()].send(address, msg);

    }

    boolean processRule(String path) {

        return send2DistributorMsg(distributorAddress, MessageTypes.MSGTYPE_NODE);
    }

    boolean processRule() {

        return processRule(getRootPath());

    }

    boolean syncTime() {

        return send2DistributorMsg(distributorAddress, MessageTypes.MSGTYPE_TIME);

    }

    boolean reset() {

        rules.clear();
        return true;

    }

}
