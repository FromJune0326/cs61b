package gitlet;


import java.io.File;
import java.io.Serializable;
import java.util.*;

import static gitlet.Utils.join;

/** Represents a gitlet commit object.
 *
 *  @author yyy
 */
public class Commit implements Serializable, Comparable {

    public static final File GITLET_DIR = join(System.getProperty("user.dir"), ".gitlet");
    public static final File COMMIT_DIR = join(GITLET_DIR, "Commit");

    public static final File BLOB_DIR = join(GITLET_DIR, "Blob");

    private String dateCreated;

    private LinkedList<String> parentHashes = new LinkedList<>();

    /** The message of this Commit. */
    private String message;

    private TreeMap<String, String> blobHashes = new TreeMap<>();

    public Commit(String msg, String date, Commit... parents) {
        message = msg;
        dateCreated = date;
        if (parents != null) {
            for (Commit parent: parents) {
                this.parentHashes.add(Utils.sha1(Utils.serialize(parent)));
            }
        }
    }

    public LinkedList<String> getParentHashes() {
        return parentHashes;
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

    public void addParentHash(String parentHash) {
        parentHashes.add(parentHash);
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
        return new TreeSet(blobHashes.keySet());
    }

    public static Commit getCommit(String hash) {
        if (hash != null) {
            if (checkCommitExist(hash)) {
                return Utils.readObject(getCommitFile(hash), Commit.class);
            }
        }
        return null;
    }

    public static Commit getCommit(File file) {
        if (file.exists()) {
            return Utils.readObject(file, Commit.class);
        } else {
            return null;
        }
    }

    public static void writeCommit(Commit commit) {
        File commitFile = getCommitFile(commit.getHash());
        Utils.writeObject(commitFile, commit);
    }

    public static boolean checkCommitExist(String hash) {
        return Utils.join(COMMIT_DIR, hash.substring(0, 2), hash.substring(2)).exists();
    }

    public static File getCommitFile(String hash) {
        File commitDir = Utils.join(COMMIT_DIR, hash.substring(0, 2));
        if (!commitDir.exists()) {
            commitDir.mkdirs();
        }
        return Utils.join(COMMIT_DIR, hash.substring(0, 2), hash.substring(2));
    }

    public static File getBlobFile(String hash, String fileName) {
        File blobDir = Utils.join(BLOB_DIR, hash.substring(0, 2), hash.substring(2));
        if (!blobDir.exists()) {
            blobDir.mkdirs();
        }
        return Utils.join(blobDir, fileName);
    }


    @Override
    public int compareTo(Object o) {
        Commit otherCommit = (Commit)o;
        return getHash().compareTo(otherCommit.getHash());
    }
}
