package bankor;

/**
 * Created by akgunduz on 03/10/15.
 */
public abstract class Content implements ContentCallback {

    boolean valid;

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }
}
