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
    private String absPath;
    private String md5Path;
    private ByteBuffer md5 = ByteBuffer.allocate(MD5_DIGEST_LENGTH);
    private boolean flaggedToSent;
    private FileTypes fileType;

    public FileContent() {

        setValid(false);
        setFlaggedToSent(true);
    }

    public FileContent(Unit host, Unit node, String rootPath, String path, FileTypes type) {

        //TODO check file first

        setFile(host, node, rootPath, path, fileType);

        setValid(true);
        setFlaggedToSent(true);

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

    public String getAbsPath() {
        return absPath;
    }

    public String getMD5Path() {
        return md5Path;
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

    FileTypes getFileType() {
        return fileType;
    }

    void setFile(Unit host, Unit node, String rootPath,
                 String path, FileTypes fileType) {

        this.path = path;
        this.fileType = fileType;

        if (host.getType() == HostTypes.HOST_COLLECTOR) {

            switch (fileType) {
                case FILE_RULE:
                    absPath = rootPath;
                    break;
                case FILE_COMMON:
                    absPath = rootPath + "common/";
                    break;
                case FILE_ARCH:
                    absPath = rootPath + "arch/" + ArchTypes.getDir(node.getID()) + "/";
                    break;
            }

        } else {
            absPath = rootPath;
        }

        md5Path = rootPath + "md5/";

        try {

            if (Files.notExists(Paths.get(absPath))) {
                Files.createDirectories(Paths.get(absPath));
            }

            if (Files.notExists(Paths.get(md5Path))) {
                Files.createDirectories(Paths.get(md5Path));
            }

        } catch (Exception e) {
            System.out.println("FileContent -> " + e.getMessage());
        }

        absPath += path;

        md5Path += path + ".md5";
    }
}
