package bankor;

import java.util.*;

/**
 * Created by akgunduz on 10/9/15.
 */
public class Collector extends Component {

    int distributorIndex;
    int nodeIndex;

    long distributorAddress = 0;

    Map<Long, Rule> rules = new HashMap<>();

    public Collector(int distributorIndex, int nodeIndex, String rootPath) {
        super(distributorIndex, nodeIndex, rootPath);

        this.distributorIndex = 0;
        this.nodeIndex = 1;
    }

    @Override
    public boolean onProcess(long address, Message msg) {

        switch (msg.getOwner()) {

            case HOST_DISTRIBUTOR:
                if (connectors[distributorIndex].getInterfaceType() == Address.getInterface(address)) {
                    processDistributorMsg(address, msg);
                }
                break;

            case HOST_NODE:
                if (connectors[nodeIndex].getInterfaceType() == Address.getInterface(address)) {
                    processNodeMsg(address, msg);
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

     //           LOG_U(UI_UPDATE_COLL_ATT_DIST_ADDRESS, address);

       //         LOG_U(UI_UPDATE_COLL_LOG,
       //                 "\"WAKEUP\" msg from distributor: %s", Address::getString(address).c_str());

                status = send2DistributorMsg(address, MessageTypes.MSGTYPE_READY);
                break;

            case MSGTYPE_NODE:

                long clientAddress = msg.getVariant(0);

                if (clientAddress == 0) {
         //           LOG_W("No available client right now.");
                    status = false;
         //           LOG_U(UI_UPDATE_COLL_LOG,
        //                    "\"CLIENT\" msg from distributor: %s, no Available Client", Address::getString(address).c_str());
                    break;
                }

                Rule rule = new Rule(getRootPath(), Rule.RULE_FILE);
                if (!rule.isValid()) {
                    //LOG_E("Could not create a rule from path : %s", getRootPath().c_str());
                    return false;
                }

                rules.put(clientAddress, rule);

            //    LOG_T("New Rule created from path : %s", getRootPath().c_str());

         //       LOG_U(UI_UPDATE_COLL_FILE_LIST, rules[clientAddress]);
         //       LOG_U(UI_UPDATE_COLL_PARAM_LIST, rules[clientAddress]);
         //       LOG_U(UI_UPDATE_COLL_EXEC_LIST, rules[clientAddress]);

        //        LOG_U(UI_UPDATE_COLL_ATT_CLIENT_ADDRESS, clientAddress);
        //        LOG_U(UI_UPDATE_COLL_LOG,
        //                "\"CLIENT\" msg from distributor: %s, available client: %s",
       //                 Address::getString(address).c_str(), Address::getString(clientAddress).c_str());

                status = send2NodeMsg(clientAddress, MessageTypes.MSGTYPE_RULE);
                break;

            default:
                break;
        }

        return status;

    }

    boolean processNodeMsg(long address, Message msg) {

        boolean status = false;

        switch(msg.getType()) {

            case MSGTYPE_MD5:

                //           LOG_U(UI_UPDATE_COLL_LOG,
                //                  "\"MD5\" msg from client: %s with \"%d\" MD5 info",
                //                   Address::getString(address).c_str(), msg->mMD5List.size());

                Rule rule = rules.get(address);
                if (rule == null) {
                    //               LOG_W("Could not find a rule for address : %lld", address);
                    break;
                }

                for (int i = 0; i < msg.md5List.size(); i++) {
                    //TODO contentler icinde ara
                    for (int j = 0; j < rule.getContentCount(RuleTypes.RULE_FILES); j++) {
                        FileContent content = (FileContent) rule.getContent(RuleTypes.RULE_FILES, j);
                        //TODO 64 bit le coz
                        if (content.getMD5().equals(msg.md5List.get(i))) {
                            //TODO bu clientte var, gonderme
                            content.setFlaggedToSent(false);
                        }
                    }
                }

                //            LOG_U(UI_UPDATE_COLL_FILE_LIST, ruleItr->second);

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
     //       LOG_W("Could not find a rule for address : %lld", address);
            return false;
        }

        switch(type) {

            case MSGTYPE_RULE:

                msg.setRule(Message.STREAM_RULE, rule);
       //         LOG_U(UI_UPDATE_COLL_LOG,
          //              "\"RULE\" msg sent to client: %s",
       //                 Address::getString(address).c_str());
                break;

            case MSGTYPE_BINARY:

                msg.setRule(Message.STREAM_BINARY, rule);
    //            LOG_U(UI_UPDATE_COLL_LOG,
     //                   "\"BINARY\" msg sent to client: %s with \"%d\" file binary",
    //                    Address::getString(address).c_str(), ruleItr->second->getFlaggedFileCount());
                break;

            default:
                return false;

        }

        return connectors[nodeIndex].send(address, msg);

    }

    boolean send2DistributorMsg(long address, MessageTypes type) {

        Message msg = new Message(HostTypes.HOST_COLLECTOR, type, getRootPath());

        switch(type) {

            case MSGTYPE_READY:

        //        LOG_U(UI_UPDATE_COLL_LOG,
       //                 "\"READY\" msg sent to distributor: %s",
        //                Address::getString(address).c_str());
                break;

            case MSGTYPE_NODE:

    //            LOG_U(UI_UPDATE_COLL_LOG,
     //                   "\"CLIENT\" msg sent to distributor: %s",
     //                   Address::getString(address).c_str());
                break;

            case MSGTYPE_TIME:

    //            LOG_U(UI_UPDATE_COLL_LOG,
     //                   "\"TIME\" msg sent to distributor: %s",
      //                  Address::getString(address).c_str());
                break;

            default:
                return false;
        }

        return connectors[distributorIndex].send(address, msg);

    }

    boolean processRule(String path) {

        if (!path.equals(getRootPath())) {
            setRootPath(path);
        }

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
