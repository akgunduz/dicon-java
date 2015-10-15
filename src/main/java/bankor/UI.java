package bankor;

import javafx.application.Platform;

/**
 * Created by akgunduz on 30/08/15.
 */
public enum UI {

    LOG,

    DIST_ADDRESS,
    DIST_COLL_LIST,
    DIST_NODE_LIST,
    DIST_BACKUP,
    DIST_LOG,

    COLL_ADDRESS,
    COLL_ATT_DIST_ADDRESS,
    COLL_ATT_NODE_ADDRESS,
    COLL_FILE_LIST,
    COLL_EXEC_LIST,
    COLL_LOG,

    NODE_ADDRESS,
    NODE_STATE,
    NODE_ATT_COLL_ADDRESS,
    NODE_FILE_LIST,
    NODE_EXEC_LIST,
    NODE_LOG;

    void update(final Object... args) {

        class UpdateTask implements Runnable {

            public void run() {

                updaters[ordinal()].onUpdate(args);
            }
        }

        Platform.runLater(new UpdateTask());
    }

    private static LogCallback updaters[] = new LogCallback[values().length];

    public static void set(LogCallback... cbs) {

        System.arraycopy(cbs, 0, updaters, 0, cbs.length);
    }
}
