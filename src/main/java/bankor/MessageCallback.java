package bankor;

import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Created by akgunduz on 11/09/15.
 */
public interface MessageCallback {

    boolean readMessageBlock(ReadableByteChannel in, BaseMessage.BlockHeader blockHeader);
    boolean writeMessageStream(WritableByteChannel out, int streamFlag);
}
