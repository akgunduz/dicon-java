package bankor;

import java.io.File;
import java.nio.file.Files;

/**
 * Created by akgunduz on 10/11/15.
 */
public class Node extends Component {

    public static final String NODE_PATH = "Node";

    long distributorAddress = 0;

    Rule rule;

    public Node(int distributorIndex, int collectorIndex, String rootPath) {
        super(generateIndex(distributorIndex, collectorIndex, 0xFFFF), rootPath);

        UI.NODE_ADDRESS.update(connectors[HostTypes.HOST_DISTRIBUTOR.getId()].getAddress(),
                connectors[HostTypes.HOST_COLLECTOR.getId()].getAddress());
    }

    @Override
    public boolean processDistributorMsg(long address, Message msg) {

        boolean status = false;

        switch(msg.getType()) {

            case MSGTYPE_WAKEUP:

                distributorAddress = address;

                UI.NODE_LOG.update("\"WAKEUP\" msg from distributor: " + Address.getString(address));

                status = send2DistributorMsg(address, MessageTypes.MSGTYPE_ALIVE);
                break;

            default:
                break;
        }

        return status;

    }

    @Override
    public boolean processCollectorMsg(long address, Message msg) {

        boolean status = false;

        switch(msg.getType()) {

            case MSGTYPE_RULE:

                UI.NODE_STATE.update(NodeStates.BUSY);
                UI.NODE_LOG.update("\"RULE\" msg from collector: " + Address.getString(address));

                status = send2DistributorMsg(distributorAddress, MessageTypes.MSGTYPE_BUSY);

                rule = msg.getRule();

                if (!processMD5()) {
                    UI.NODE_LOG.update("Processing MD5 failed!!!");
                    break;
                }

                UI.NODE_ATT_COLL_ADDRESS.update(address);
                UI.NODE_FILE_LIST.update(rule);
                UI.NODE_EXEC_LIST.update(rule);

                status &= send2CollectorMsg(address, MessageTypes.MSGTYPE_MD5);
                break;

            case MSGTYPE_BINARY:

                rule = msg.getRule();
                if (rule == null) {
                    rule = new Rule(getRootPath(), Rule.RULE_FILE);
                }

                UI.NODE_FILE_LIST.update(rule);
                UI.NODE_LOG.update("\"BINARY\" msg from collector: " + Address.getString(address));

                processRule();

                UI.NODE_STATE.update(NodeStates.IDLE);

                status = send2DistributorMsg(distributorAddress, MessageTypes.MSGTYPE_READY);
                break;

            default:
                break;
        }

        return status;

    }

    @Override
    public boolean processNodeMsg(long address, Message msg) {
        return false;
    }

    boolean send2DistributorMsg(long address, MessageTypes type) {

        Message msg = new Message(HostTypes.HOST_NODE, type, getRootPath());

        switch(type) {

            case MSGTYPE_READY:

                UI.NODE_LOG.update("\"READY\" msg sent to distributor: " + Address.getString(address));
                break;

            case MSGTYPE_ALIVE:

                UI.NODE_LOG.update("\"ALIVE\" msg sent to distributor: " + Address.getString(address));
                break;

            case MSGTYPE_BUSY:

                UI.NODE_LOG.update("\"BUSY\" msg sent to distributor: " + Address.getString(address));
                break;

            default:
                return false;
        }

        return connectors[HostTypes.HOST_DISTRIBUTOR.getId()].send(address, msg);

    }

    boolean send2CollectorMsg(long address, MessageTypes type) {

        Message msg = new Message(HostTypes.HOST_NODE, type, getRootPath());

        switch(type) {

            case MSGTYPE_MD5:

                msg.setRule(Message.STREAM_MD5ONLY, rule);
                UI.NODE_LOG.update("\"MD5\" msg sent to collector: " + Address.getString(address) +
                    " with MD5 count: " + rule.getFlaggedFileCount());
                break;

            default:
                return false;
        }

        return connectors[HostTypes.HOST_COLLECTOR.getId()].send(address, msg);

    }

    boolean processMD5() {

        for (int i = 0; i < rule.getContentCount(RuleTypes.RULE_FILES); i++) {

            FileContent content = (FileContent) rule.getContent(RuleTypes.RULE_FILES, i);

            String abspath = getRootPath() + content.getPath();

            //Burada ters lojik var,
            //	false -> collectordan dosyayi iste, md5 i set ETMEYEREK
            //	true -> collectordan dosyayi isteme, md5 i set EDEREK

            File f = new File(abspath);
            if(!f.exists() || f.isDirectory()) {
                content.setFlaggedToSent(false);
            }

        }

        return true;

    }

    void processExecutor(String cmd) {

        UI.NODE_LOG.update("Executing command: " + cmd);

        try {

            Runtime.getRuntime().exec(cmd);

        } catch (Exception e) {

            UI.NODE_LOG.update("Execution: " + cmd + " failed!!!");
        }
    }

    boolean processRule() {

        if (!rule.isParallel()) {

            for (int i = 0; i < rule.getContentCount(RuleTypes.RULE_EXECUTORS); i++) {
                ExecutorContent content = (ExecutorContent) rule.getContent(RuleTypes.RULE_EXECUTORS, i);
                String cmd = content.getParsed(rule);
                File executable = new File(cmd.substring(0, cmd.indexOf(' ')));
                executable.setExecutable(true);
                processExecutor(cmd);
            }

        }

        return true;
    }
}
