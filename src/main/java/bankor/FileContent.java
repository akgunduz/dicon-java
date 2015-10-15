package bankor;

import javax.xml.bind.DatatypeConverter;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;

/**
 * Created by akgunduz on 03/10/15.
 */
public class FileContent extends Content {

    private static final int MD5_DIGEST_LENGTH = 16;

    private String path;
    private ByteBuffer md5 = ByteBuffer.allocate(MD5_DIGEST_LENGTH);
    private boolean flaggedToSent;

    public FileContent() {

        setValid(false);
        flaggedToSent = true;
    }

    public FileContent(String rootPath, String path, String sMD5) {

        this.path = path;
        String absPath = rootPath + path;

        setValid(true);
        flaggedToSent = true;

        if (sMD5 != null && !sMD5.isEmpty()) {
            md5.put(Util.strToHex(sMD5));
            return;
        }

        String md5Path = absPath + ".md5";

        try {

            byte[] buffer = new byte[MD5_DIGEST_LENGTH * 2 + 1];
            RandomAccessFile f = new RandomAccessFile(md5Path, "r");
            f.read(buffer);
            f.close();
            md5.put(Util.strToHex(new String(buffer)));


        } catch (Exception e) {

            System.out.println("FileContent -> " + e.getMessage());

            try {

                MessageDigest ctx = MessageDigest.getInstance("MD5");
                InputStream is = Files.newInputStream(Paths.get(absPath));
                DigestInputStream dis = new DigestInputStream(is, ctx);

                byte[] tmp = new byte[1024];

                int count;
                do {
                    count = dis.read(tmp);
                } while (count != -1);
                md5.put(ctx.digest());

                dis.close();
                is.close();

                RandomAccessFile f = new RandomAccessFile(md5Path, "w");
                f.write(DatatypeConverter.printHexBinary(md5.array()).getBytes());
                f.close();

            } catch (Exception e1) {

                System.out.println("FileContent -> " + e1.getMessage());
                setValid(false);
            }
        }
    }

    @Override
    public ContentTypes getType() {
        return ContentTypes.CONTENT_FILE;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public ByteBuffer getMD5() {
        return md5;
    }

    public void setMD5(ByteBuffer md5) {
        this.md5 = md5;
    }

    public boolean isFlaggedToSent() {
        return flaggedToSent;
    }

    public void setFlaggedToSent(boolean flaggedToSent) {
        this.flaggedToSent = flaggedToSent;
    }

    public void set(FileContent content) {

        path = content.getPath();
        md5 = content.getMD5();
        flaggedToSent = content.isFlaggedToSent();
        setValid(content.isValid());
    }
}
