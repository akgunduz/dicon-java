package bankor;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;

/**
 * Created by akgunduz on 11/09/15.
 */
public class Message extends BaseMessage {

    private static final int STREAM_RULE = 0x01;
    private static final int STREAM_BINARY = 0x02;
    private static final int STREAM_MD5ONLY = 0x03;

    private static final int BLOCK_FILE_BINARY = 0x01;
    private static final int BLOCK_FILE_MD5 = 0x02;

    private static final int MD5_DIGEST_LENGTH = 16;


    private Rule rule;
    private String rootPath;
    private List<ByteBuffer> md5List;

    public Message(String rootPath) {

        this.rule = null;
        this.rootPath = rootPath;
    }

    public Message(HostTypes owner, int type, String rootPath) {

        super(owner.getId(), type);
        this.rule = null;
        this.rootPath = rootPath;

        setStreamFlag(STREAM_NONE);
    }

    HostTypes getOwner() {
        return HostTypes.getHost((int)getDeviceID());
    }

    void setRule(int streamFlag, Rule rule) {

        setStreamFlag(streamFlag);

        this.rule = rule;
    }

    boolean readMD5(ReadableByteChannel in, ByteBuffer md5) {

        if (!readBlock(in, md5, MD5_DIGEST_LENGTH)) {
            System.out.println("Message.readMD5 -> " + "can not read block");
            return false;
        }

        return true;
    }

    boolean readFileBinary(ReadableByteChannel in, BlockHeader header, FileContent content) {

        if (header.blockType != BLOCK_FILE_BINARY) {
            System.out.println("Message.readFileBinary -> " + "can not read block");
            return false;
        }

        StringBuilder path = new StringBuilder("");
        if (!readString(in, path, header.sizes[0])) {
            System.out.println("Message.readFileBinary -> " + "can not read path data");
            return false;
        }
        content.setPath(path.toString());

        if (header.sizes[1] != MD5_DIGEST_LENGTH) {
            System.out.println("Message.readFileBinary -> " + "Md5 size must be" + MD5_DIGEST_LENGTH + " long");
            return false;
        }

        ByteBuffer md5 = ByteBuffer.allocate(MD5_DIGEST_LENGTH);
        if (!readMD5(in, md5)) {
            System.out.println("Message.readFileBinary -> " + "can not read MD5 data");
            return false;
        }
        content.setMD5(md5);

        ByteBuffer calcmd5 = ByteBuffer.allocate(MD5_DIGEST_LENGTH);
        if (!readBinary(in, rootPath + path, calcmd5, header.sizes[2])) {
            System.out.println("Message.readFileBinary -> " + "can not read Binary data");
            return false;
        }

        if (!md5.equals(calcmd5)) {
            System.out.println("Message.readFileBinary -> " + "mismatch in md5 data");
            return false;
        }
        content.setValid(true);

        return true;
    }

    boolean readFileMD5(ReadableByteChannel in, BlockHeader header, ByteBuffer md5) {

        if (header.sizes[0] != MD5_DIGEST_LENGTH) {
            System.out.println("Message.readFileMD5 -> " + "mismatch in md5 data");
            return false;
        }

        if (!readMD5(in, md5)) {
            System.out.println("Message.readFileMD5 -> " + "can not read md5 data");
            return false;
        }

        return true;
    }

    @Override
    public boolean readMessageBlock(ReadableByteChannel in, BlockHeader blockHeader) {

        switch(blockHeader.blockType) {

            case BLOCK_FILE_BINARY:

                FileContent fileContent = new FileContent();

                if (!readFileBinary(in, blockHeader, fileContent)) {
                    return false;
                }

                if (rule == null) {

                    rule = new Rule(rootPath);

                }

                if (rule.isValid() && fileContent.getPath().equals(Rule.RULE_FILE)) {

                    rule.updateFileContent(fileContent);

                }

                break;

            case BLOCK_FILE_MD5:
                    ByteBuffer md5 = ByteBuffer.allocate(MD5_DIGEST_LENGTH);
                    if (!readFileMD5(in, blockHeader, md5)) {
                    return false;
                }

                md5List.add(md5);
                break;

            default:
                return false;
        }

        return true;
    }

    boolean writeMD5(WritableByteChannel out, ByteBuffer md5) {

        if (!writeBlock(out, md5, MD5_DIGEST_LENGTH)) {
            System.out.println("Message.writeFileBinary -> " + "can not write md5 data");
            return false;
        }

        return true;
    }

    boolean writeFileBinary(WritableByteChannel out, FileContent content) {

        final BlockHeader blockHeader = new BlockHeader(3, BLOCK_FILE_BINARY);

        blockHeader.sizes[0] = content.getPath().length();
        blockHeader.sizes[1] = MD5_DIGEST_LENGTH;
        blockHeader.sizes[2] = getBinarySize(rootPath + content.getPath());

        if (!writeBlockHeader(out, blockHeader)) {
            return false;
        }

        if (!writeString(out, content.getPath())) {
            return false;
        }

        if (!writeMD5(out, content.getMD5())) {
            return false;
        }

        ByteBuffer calcmd5 = ByteBuffer.allocate(MD5_DIGEST_LENGTH);
        if (!writeBinary(out, rootPath + content.getPath(), calcmd5)) {
            System.out.println("Message.writeFileBinary -> " + "can not write Binary data");
            return false;
        }

        if (!content.getMD5().equals(calcmd5)) {
            System.out.println("Message.readFileBinary -> " + "mismatch in md5 data");
            return false;
        }

        return true;

    }

    boolean writeFileMD5(WritableByteChannel out, FileContent content) {

        final BlockHeader blockHeader = new BlockHeader(1, BLOCK_FILE_MD5);

        blockHeader.sizes[0] = MD5_DIGEST_LENGTH;

        if (!writeBlockHeader(out, blockHeader)) {
            return false;
        }

        return writeMD5(out, content.getMD5());
    }

    @Override
    public boolean writeMessageStream(WritableByteChannel out, int streamFlag) {

        switch(streamFlag) {

            case STREAM_RULE:
                if (!writeFileBinary(out, rule.getRuleFile())) {
                    return false;
                }
                break;

            case STREAM_BINARY:
                for (int i = 0; i < rule.getContentCount(RuleTypes.RULE_FILES); i++) {
                    FileContent content = (FileContent) rule.getContent(RuleTypes.RULE_FILES, i);
                    if (content.isFlaggedToSent()) {
                        if (!writeFileBinary(out, content)) {
                            return false;
                        }
                    }
                }
                break;

            case STREAM_MD5ONLY:
                for (int i = 0; i < rule.getContentCount(RuleTypes.RULE_FILES); i++) {
                    FileContent content = (FileContent) rule.getContent(RuleTypes.RULE_FILES, i);
                    if (content.isFlaggedToSent()) {
                        if (!writeFileMD5(out, content)) {
                            return false;
                        }
                    }
                }
                break;

            default :
                break;
        }

        return true;
    }
}
