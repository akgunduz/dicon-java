package bankor;

import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Created by akgunduz on 11/09/15.
 */
public interface ContentCallback {

    ContentTypes getType();
}
