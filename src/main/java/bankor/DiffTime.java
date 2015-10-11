package bankor;

/**
 * Created by akgunduz on 10/11/15.
 */
public class DiffTime {

    private boolean initiated = false;
    double tStart;

    void start() {

        tStart = System.currentTimeMillis();
        initiated = true;
    }

    double stop() {

        if (!initiated) {
            return 0;
        }

        return System.currentTimeMillis() - tStart;
    }

    void reset() {

        initiated = false;
    }

    boolean isInitiated() {

        return initiated;
    }
}
