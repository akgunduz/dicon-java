package bankor;

import java.io.File;

/**
 * Created by akgunduz on 10/11/15.
 */
public class Node extends Component {

    int distributorIndex;
    int collectorIndex;

    long distributorAddress = 0;

    Rule rule;

    public Node(int distributorIndex, int collectorIndex, String rootPath) {
        super(distributorIndex, collectorIndex, rootPath);

        this.distributorIndex = distributorIndex;
        this.collectorIndex = collectorIndex;
    }

    @Override
    public boolean onProcess(long address, Message msg) {

        switch (msg.getOwner()) {

            case HOST_DISTRIBUTOR:
                if (connectors[distributorIndex].getInterfaceType() == Address.getInterface(address)) {
                    processDistributorMsg(address, msg);
                }
                break;

            case HOST_COLLECTOR:
                if (connectors[collectorIndex].getInterfaceType() == Address.getInterface(address)) {
                    processCollectorMsg(address, msg);
                }
                break;

            default:
                System.out.println("Wrong message : " + msg.getType().getName() + "received from " + Address.getString(address));
                break;

        }

        return true;
    }

    boolean processDistributorMsg(long address, Message msg) {

        boolean status = false;

        switch(msg.getType()) {

            case MSGTYPE_WAKEUP:

                distributorAddress = address;

        //        LOG_U(UI_UPDATE_CLIENT_LOG,
        //                "\"WAKEUP\" msg from distributor: %s", Address::getString(address).c_str());

                status = send2DistributorMsg(address, MessageTypes.MSGTYPE_ALIVE);
                break;

            default:
                break;
        }

        return status;

    }

    boolean processCollectorMsg(long address, Message msg) {

        boolean status = false;

        switch(msg.getType()) {

            case MSGTYPE_RULE:

             //   LOG_U(UI_UPDATE_CLIENT_STATE, BUSY);
                status = send2DistributorMsg(distributorAddress, MessageTypes.MSGTYPE_BUSY);

         //       LOG_U(UI_UPDATE_CLIENT_LOG,
         //               "\"RULE\" msg from collector: %s", Address::getString(address).c_str());

                rule = msg.getRule();

                if (!processMD5()) {
             //       LOG_E("Processing MD5 failed!!!");
                    break;
                }

         //       LOG_U(UI_UPDATE_CLIENT_FILE_LIST, mRule);
         //       LOG_U(UI_UPDATE_CLIENT_PARAM_LIST, mRule);
          //      LOG_U(UI_UPDATE_CLIENT_EXEC_LIST, mRule);

                status &= send2CollectorMsg(address, MessageTypes.MSGTYPE_MD5);
                break;

            case MSGTYPE_BINARY:

                rule = msg.getRule();
                if (rule == null) {
                    rule = new Rule(getRootPath(), Rule.RULE_FILE);
                }

         //       LOG_U(UI_UPDATE_CLIENT_LOG,
         //               "\"BINARY\" msg from collector: %s with \"%d\" file binary",
         //               Address::getString(address).c_str(), 0/*msg->getReceivedBinaryCount()*/);

      //          LOG_U(UI_UPDATE_CLIENT_FILE_LIST, mRule);

                processRule();

       //         LOG_U(UI_UPDATE_CLIENT_STATE, IDLE);
                status = send2DistributorMsg(distributorAddress, MessageTypes.MSGTYPE_READY);
                break;

            default:
                break;
        }

        return status;

    }

    boolean send2DistributorMsg(long address, MessageTypes type) {

        Message msg = new Message(HostTypes.HOST_COLLECTOR, type, getRootPath());

        switch(type) {

            case MSGTYPE_READY:

            //    LOG_U(UI_UPDATE_CLIENT_LOG,
           //             "\"READY\" msg sent to distributor: %s",
           //             Address::getString(address).c_str());
                break;

            case MSGTYPE_ALIVE:

     //           LOG_U(UI_UPDATE_CLIENT_LOG,
     //                   "\"ALIVE\" msg sent to distributor: %s",
    //                    Address::getString(address).c_str());
                break;

            case MSGTYPE_BUSY:

       //         LOG_U(UI_UPDATE_CLIENT_LOG,
       //                 "\"BUSY\" msg sent to distributor: %s",
        //                Address::getString(address).c_str());
                break;

            default:
                return false;
        }

        return connectors[distributorIndex].send(address, msg);

    }

    boolean send2CollectorMsg(long address, MessageTypes type) {

        Message msg = new Message(HostTypes.HOST_DISTRIBUTOR, type, getRootPath());

        switch(type) {

            case MSGTYPE_MD5:

                msg.setRule(Message.STREAM_MD5ONLY, rule);
        //        LOG_U(UI_UPDATE_CLIENT_LOG,
        //                "\"MD5\" msg sent to collector: %s with \"%d\" MD5 info",
         //               Address::getString(address).c_str(), mRule->getFlaggedFileCount());
                break;

            default:
                return false;
        }

        return connectors[collectorIndex].send(address, msg);

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

 //       LOG_I("ExecV run with cmd : %s", cmdargs[0]);
        try {
            Runtime.getRuntime().exec(cmd);

        } catch (Exception e) {
            //      LOG_E("ExecV failed with error : %d", errno);
        }
    }

    boolean processRule() {

        if (!rule.isParallel()) {

            for (int i = 0; i < rule.getContentCount(RuleTypes.RULE_EXECUTORS); i++) {
                ExecutorContent content = (ExecutorContent) rule.getContent(RuleTypes.RULE_EXECUTORS, i);
                String cmd = content.getParsed(rule);
          //      LOG_U(UI_UPDATE_CLIENT_LOG,
          //              "Executing %s command", cmd.c_str());
                processExecutor(cmd);
            }

        }

        return true;
    }
}
