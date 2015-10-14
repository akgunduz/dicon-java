package bankor;

import javafx.application.Platform;

/**
 * Created by akgunduz on 10/12/15.
 */
public class Log {

    void updateUI(final int id, final Object... args) {

        class UpdateTask implements Runnable {

            public void run() {


            }
        }

        Platform.runLater(new UpdateTask());
    }


}
