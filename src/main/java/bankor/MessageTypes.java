package bankor;

/**
 * Created by akgunduz on 30/08/15.
 */
public enum MessageTypes {

    MSGTYPE_NODE(100, "MSGTYPE_NODE"),
    MSGTYPE_RULE(101, "MSGTYPE_RULE"),
    MSGTYPE_BINARY(102, "MSGTYPE_BINARY"),
    MSGTYPE_MD5(103, "MSGTYPE_MD5"),
    MSGTYPE_WAKEUP(104, "MSGTYPE_WAKEUP"),
    MSGTYPE_READY(105, "MSGTYPE_READY"),
    MSGTYPE_BUSY(106, "MSGTYPE_BUSY"),
    MSGTYPE_TIME(107, "MSGTYPE_TIME"),
    MSGTYPE_TIMEOUT(108, "MSGTYPE_TIMEOUT"),
    MSGTYPE_ALIVE(109, "MSGTYPE_ALIVE");

    int id;
    String name;

    MessageTypes(int id, String name) {
        this.id = id;
        this.name = name;
    }

    int getId() {
        return id;
    }

    String getName() {
        return name;
    }

    public static MessageTypes getMessage(int id) {
        return values()[id - MSGTYPE_NODE.id];
    }

    public static String getName(int id) {
        return getMessage(id).getName();
    }
}
