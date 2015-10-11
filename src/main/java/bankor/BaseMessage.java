package bankor;

/**
 * Created by akgunduz on 30/08/15.
 */

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.MessageDigest;
import java.util.Random;

public abstract class BaseMessage implements MessageCallback {

    private static final short BUFFER_SIZE = 512;

    private static final short PRIORITY_COEFFICIENT = 10;

    private static final short DEFAULT_PRIORITY = 3;

    private static final short SIGNATURE = 0x55AA;

    protected static final int STREAM_NONE = 0xFFFF;

    private static final int BLOCK_END_STREAM = 0xFFFF;

    private static final int BLOCK_HEADER_SIZE = 8;

    private static final int TMP_BUFFER_SIZE = 1000;

    private static final int MAX_VARIANT = 2;

    private static final int MESSAGE_HEADER_SIZE = 40 + MAX_VARIANT * 8;

    private static final int MD5_DIGEST_LENGTH = 16;


    public class MessageHeader {

        int type;
        int priority;
        long ownerAddress;
        long time;
        long deviceID;
        long messageID;
        long[] variant = new long[MAX_VARIANT];
    }

    public class BlockHeader {

        int blockType;
        int blockCount;
        int[] sizes;

        BlockHeader() {

        }

        BlockHeader(int blockCount) {

            this.blockCount = blockCount;
            if (blockCount > 0) {
                sizes = new int[blockCount];
            }
        }

        BlockHeader(int blockCount, int blockType) {

            this(blockCount);
            this.blockType = blockType;
        }
    }

    ByteBuffer tmpBuf = ByteBuffer.allocate(TMP_BUFFER_SIZE);
    MessageHeader header = new MessageHeader();

    int streamFlag;

    public BaseMessage() {

        setStreamFlag(STREAM_NONE);
        setPriority(DEFAULT_PRIORITY);
    }

    public BaseMessage(long deviceID, int type) {

        this();
        header.time = Util.getTime().getTime();
        header.deviceID = deviceID;
        header.messageID = new Random(header.time).nextLong();
        header.type = type;

    }

    void setStreamFlag(int flag) {
        streamFlag = flag;
    }

    MessageTypes getType() {
        return MessageTypes.getMessage(header.type);
    }

    long getOwnerAddress() {
        return header.ownerAddress;
    }

    void setOwnerAddress(long address) {
        header.ownerAddress = address;
    }

    long getTime() {
        return header.time;
    }

    long getDeviceID() {
        return header.deviceID;
    }

    long getMessageID() {
        return header.messageID;
    }

    long getVariant(int id) {

        if (id < MAX_VARIANT) {
            return header.variant[id];
        }

        return 0;
    }

    void setVariant(int id, long variant) {

        if (id >= MAX_VARIANT) {
            return;
        }

        header.variant[id] = variant;
    }

    int getPriority() {
        return header.priority;
    }

    int iteratePriority() {

        if (header.priority > 1) {
            header.priority--;
        }

        return header.priority;
    }

    void setPriority(int priority) {
        header.priority = priority;
    }

    void normalizePriority() {
        header.priority *= PRIORITY_COEFFICIENT;
    }


    int getBinarySize(final String path) {

        File f = new File(path);
        if (!f.exists()) {
            System.out.println("Message.getBinarySize -> " + "File " + path + " could not found");
            return 0;
        }

        return (int)f.length();
    }

    boolean transferBinary(ReadableByteChannel in, WritableByteChannel out, ByteBuffer md5, int size) {

        boolean error = false;

        ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);

        try {

            MessageDigest ctx = MessageDigest.getInstance("MD5");

            int readSize;

            do {

                if (size > BUFFER_SIZE) {
                    readSize = BUFFER_SIZE;
                    size -= readSize;
                } else {
                    readSize = size;
                    size = 0;
                }

                if (!readBlock(in, buf, readSize)) {
                    System.out.println("Message.transferBinary -> " + "Can not read data");
                    error = true;
                    break;
                }

                if (!writeBlock(out, buf, readSize)) {
                    System.out.println("Message.transferBinary -> " + "Can not write data");
                    error = true;
                    break;
                }

                ctx.update(buf);

            } while (size > 0);

            md5.put(ctx.digest());

        } catch (Exception e) {

            System.out.println("Message.transferBinary -> " + e.getMessage());
        }

        return !error;

    }

    boolean readBlock(ReadableByteChannel in, ByteBuffer buf, int size) {

        buf.clear();
        buf.limit(size);

        boolean busy = false;

        do {

            long count;

            try {

                count = in.read(buf);

            } catch (Exception e) {

                if (e instanceof IOException && !busy) {

                    System.out.println("Message.readBlock -> " + e.getMessage());
                    busy = true;

                    try {
                        Thread.sleep(200);

                    } catch (Exception e1) {
                        System.out.println("Message.readBlock -> " + e1.getMessage());
                    }

                    continue;

                }

                System.out.println("Message.readBlock -> " + "Can not read data block");
                return false;
            }

            if (count == 0 || count == -1) {
                System.out.println("Message.readBlock -> " + "Empty read operation");
                return false;
            }

            if (count < size) {
                size -= count;

                if (size == 0) {
                    break;
                }

                busy = false;
                continue;
            }

            break;

        } while(true);

        buf.rewind();

        return true;
    }

    boolean readSignature(ReadableByteChannel in) {

        ByteBuffer buf = ByteBuffer.allocate(2);

        if (!readBlock(in, buf, 2)) {
            System.out.println("Message.readSignature -> " + "Can not read correct signature from stream");
            return false;
        }

        if (buf.getShort() != SIGNATURE) {
            System.out.println("Message.readSignature -> " + "Can not read correct signature from stream");
            return false;
        }

        return true;
    }

    boolean readHeader(ReadableByteChannel in, MessageHeader header) {

        if (!readBlock(in, tmpBuf, MESSAGE_HEADER_SIZE)) {
            System.out.println("Message.readHeader -> " + "Can not read message header from stream");
            return false;
        }

        header.type = tmpBuf.getInt();
        header.priority = tmpBuf.getInt();
        header.ownerAddress = tmpBuf.getLong();
        header.time = tmpBuf.getLong();
        header.deviceID = tmpBuf.getLong();
        header.messageID = tmpBuf.getLong();

        return true;
    }

    boolean readBlockHeader(ReadableByteChannel in, BlockHeader header) {

        if (!readBlock(in, tmpBuf, BLOCK_HEADER_SIZE)) {
            System.out.println("Message.readBlockHeader -> " + "Can not read block header from stream");
            return false;
        }

        header.blockType = tmpBuf.getInt();
        header.blockCount = tmpBuf.getInt();

        if (header.blockCount > 0) {

            header.sizes = new int[header.blockCount];

            if (!readBlock(in, tmpBuf, header.blockCount * 4)) {
                System.out.println("Message.readBlockHeader -> " + "Can not read block header from stream");
                return false;
            }

            for (int i = 0; i < header.blockCount; i++) {
                header.sizes[i] = tmpBuf.getInt();
            }
        }

        return true;
    }

    boolean readString(ReadableByteChannel in, StringBuilder object, int size) {

        object.setLength(0);

        while(size > TMP_BUFFER_SIZE - 1) {

            if (!readBlock(in, tmpBuf, TMP_BUFFER_SIZE - 1)) {
                System.out.println("Message.readString -> " + "Can not read string data from stream");
                return false;
            }

            object.append(new String(tmpBuf.array(), 0, size));

            size -= TMP_BUFFER_SIZE + 1;
        }

        if (!readBlock(in, tmpBuf, size)) {
            System.out.println("Message.readString -> " + "Can not read string data from stream");
            return false;
        }

        object.append(new String(tmpBuf.array(), 0, size));

        return true;
    }

    boolean readNumber(ReadableByteChannel in, long[] number) {

        if (!readBlock(in, tmpBuf, 8)) {
            System.out.println("Message.readNumber -> " + "Can not read number from stream");
            return false;
        }

        number[0] = tmpBuf.getLong(0);
        return true;
    }

    boolean readBinary(ReadableByteChannel in, final String path, ByteBuffer md5, int size) {

        boolean status;

        try {

            RandomAccessFile file = new RandomAccessFile(path, "rw");

            FileChannel out = file.getChannel();

            status = transferBinary(in, out, md5, size);

            out.close();
            file.close();

            if (status && md5 != null) {

                file = new RandomAccessFile(path + ".md5", "rw");
                if (!writeBlock(out, ByteBuffer.wrap(md5.toString().getBytes()), MD5_DIGEST_LENGTH * 2)) {
                    System.out.println("Message.readBinary -> " + "Can not write md5 to file system");
                    status = false;
                }

                file.close();
            }

        } catch (Exception e) {
            System.out.println("Message.readBinary -> " + "File" + path + "could not found");
            return false;
        }

        return status;
    }

    boolean readFromStream(ReadableByteChannel in) {

        if (!readSignature(in)) {
            return false;
        }

        if (!readHeader(in, header)) {
            return false;
        }

        BlockHeader blockHeader = new BlockHeader();

        do {

            if (!readBlockHeader(in, blockHeader)) {
                return false;
            }

            if (blockHeader.blockType == BLOCK_END_STREAM) {
                return true;

            } else if (!readMessageBlock(in, blockHeader)) {
                return false;
            }

        } while(true);
    }

    boolean writeBlock(WritableByteChannel out, ByteBuffer buf, int size) {

        buf.rewind();
        buf.limit(size);

        boolean busy = false;

        do {

            long count;

            try {

                count = out.write(buf);

            } catch (Exception e) {

                if (e instanceof IOException && !busy) {
                    System.out.println("Message.writeBlock -> " + e.getMessage());
                    busy = true;

                    try {
                        Thread.sleep(200);
                    } catch (Exception e1) {
                        System.out.println("Message.writeBlock -> " + e1.getMessage());
                    }

                    continue;

                }

                System.out.println("Message.writeBlock -> " + "Can not write data block");
                return false;
            }

            if (count == 0 || count == -1) {
                System.out.println("Message.writeBlock -> " + "Empty write operation");
                return false;
            }

            if (count < size) {
                size -= count;

                if (size == 0) {
                    break;
                }

                busy = false;
                continue;
            }

            break;

        } while(true);

        return true;
    }

    boolean writeSignature(WritableByteChannel out) {

        tmpBuf.clear();
        tmpBuf.putShort(0, SIGNATURE);

        if (!writeBlock(out, tmpBuf, 2)) {
            System.out.println("Message.writeSignature -> " + "Can not write signature to stream");
            return false;
        }

        return true;
    }


    boolean writeHeader(WritableByteChannel out, MessageHeader header) {

        tmpBuf.clear();

        tmpBuf.putInt(header.type);
        tmpBuf.putInt(header.priority);
        tmpBuf.putLong(header.ownerAddress);
        tmpBuf.putLong(header.time);
        tmpBuf.putLong(header.deviceID);
        tmpBuf.putLong(header.messageID);

        if (!writeBlock(out, tmpBuf, MESSAGE_HEADER_SIZE)) {
            System.out.println("Message.writeHeader -> " + "Can not write header to stream");
            return false;
        }

        return true;
    }

    boolean writeBlockHeader(WritableByteChannel out, BlockHeader header) {

        tmpBuf.clear();

        tmpBuf.putInt(header.blockType);
        tmpBuf.putInt(header.blockCount);
        for (int i = 0; i < header.blockCount; i++) {
            tmpBuf.putInt(header.sizes[i]);
        }

        if (!writeBlock(out, tmpBuf, BLOCK_HEADER_SIZE + header.blockCount * 4)) {
            System.out.println("Message.writeBlockHeader -> " + "Can not write block header to stream");
            return false;
        }

        return true;
    }

    boolean writeString(WritableByteChannel out, String object) {

        if (!writeBlock(out, ByteBuffer.wrap(object.getBytes()), object.length())) {
            System.out.println("Message.writeString -> " + "Can not write string to stream");
            return false;
        }

        return true;
    }

    boolean writeNumber(WritableByteChannel out, long number) {

        tmpBuf.clear();
        tmpBuf.putLong(0, number);

        if (!writeBlock(out, tmpBuf, 8)) {
            System.out.println("Message.writeNumber -> " + "Can not write number to stream");
            return false;
        }

        return true;
    }

    boolean writeBinary(WritableByteChannel out, final String path, ByteBuffer md5) {

        boolean status;

        try {

            RandomAccessFile file = new RandomAccessFile(path, "r");

            FileChannel in = file.getChannel();

            status = transferBinary(in, out, md5, (int)in.size());

            in.close();
            file.close();

        } catch (Exception e) {
            System.out.println("Message.writeBinary -> " + "File " + path + " could not found");
            return false;
        }

        return status;
    }

    boolean writeEndStream(WritableByteChannel out) {

        return writeBlockHeader(out, new BlockHeader(0, BLOCK_END_STREAM));
    }

    boolean writeToStream(WritableByteChannel out) {

        if (!writeSignature(out)) {
            return false;
        }

        if (!writeHeader(out, header)) {
            return false;
        }

        if (streamFlag != STREAM_NONE) {

            if (!writeMessageStream(out, streamFlag)) {
                return false;
            }
        }

        writeEndStream(out);

        return true;
    }

}

