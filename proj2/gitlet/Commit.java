package gitlet;


import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.Set;
import java.util.TreeMap;

/** Represents a gitlet commit object.
 *
 *  @author yyy
 */
public class Commit implements Serializable {
    private String dateCreated;

    private String parentHash = null;

    /** The message of this Commit. */
    private String message;

    private TreeMap<String, String> blobHashes = new TreeMap<>();

    public Commit(String msg, String date, Commit parent) {
        message = msg;
        dateCreated = date;
        if (parent != null) {
            this.parentHash = Utils.sha1(Utils.serialize(parent));
        }
    }

    public String getParentHash() {
        return parentHash;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public String getMessage() {
        return message;
    }

    public String getHash() {
        return Utils.sha1(Utils.serialize(this));
    }

    public Commit makeCopy(String msg) {
        Commit newCommit = new Commit(msg, (new Date()).toString(), this);
        newCommit.blobHashes = (TreeMap<String, String>) blobHashes.clone();
        return newCommit;
    }

    public void addBlob(String fileName, String hash) {
        blobHashes.put(fileName, hash);
    }

    public boolean hasBlob(File file) {
        String fileName = file.getName();
        String fileHash = Utils.getFileHash(file);
        return blobHashes.containsKey(fileName) && (blobHashes.get(fileName).equals(fileHash));
    }

    public void removeBlob(String fileName) {
        blobHashes.remove(fileName);
    }

    public boolean hasFile(String fileName) {
        return blobHashes.containsKey(fileName);
    }

    public String getFileHash(String fileName) {
        if (hasFile(fileName)) {
            return blobHashes.get(fileName);
        }
        return null;
    }

    public Set<String> getFiles() {
        return blobHashes.keySet();
    }

}
