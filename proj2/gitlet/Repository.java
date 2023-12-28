package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *
 *  @author yyy
 */
public class Repository {
    /**
     * head
     * branches (dict)
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */
    static String head = null;
    static TreeMap<String, String> branches = new TreeMap<>();

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    public static final File STAGE_DIR = join(GITLET_DIR, "Stage");

    public static final File COMMIT_DIR = join(GITLET_DIR, "Commit");

    public static final File BLOB_DIR = join(GITLET_DIR, "Blob");

    public static final File COMMIT_POINTER_DIR = join(GITLET_DIR, "PTR");
    public static final File HEAD_FILE = Utils.join(COMMIT_POINTER_DIR, "HEAD");
    public static final File BRANCHES_FILE = Utils.join(COMMIT_POINTER_DIR, "BRANCHES");

    /** Init a Gitlet version-control system */
    public static void initCommit() {
        Date initDate = new Date(0);
        Commit firstCommit = new Commit("initial commit", initDate.toString(), null);
        String firstCommitHash = firstCommit.getHash();
        File firstCommitFile = getCommitFile(firstCommitHash);
        setupPersistence();
        if (!firstCommitFile.exists()) {
            writeCommit(firstCommit);
            head = firstCommitHash;
            branches.put("master", firstCommitHash);
        } else {
            Utils.message("A Gitlet version-control system already exists in the current directory.");
        }
    }

    public static void addFileToStageArea(String fileName){
        File fileToAdd = Utils.join(CWD, fileName);
        if (!fileToAdd.exists()) {
            Utils.exitWithMsg("File does not exist.");
        }
        File stagedFile = Utils.join(STAGE_DIR, fileName);
        if (getCurrentCommit().hasSameBlob(fileToAdd)) {
            if (stagedFile.exists()) {
                Utils.restrictedDelete(stagedFile);
            }
        } else {
            try {
                Files.copy(fileToAdd.toPath(), stagedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void makeNewCommit(String message) {
        List<String> stagedFiles = Utils.plainFilenamesIn(STAGE_DIR);
        if (stagedFiles == null) {
            Utils.exitWithMsg("No changes added to the commit.");
        }
        if (message.isEmpty()) {
            Utils.exitWithMsg("Please enter a commit message.");
        }
        Commit curCommit = getCurrentCommit();
        Commit newCommit = curCommit.makeCopy(message);
        /* Update blobs according to staging area */
        for (String fileName: stagedFiles) {
            File file = new File(STAGE_DIR, fileName);
            String fileHash = Utils.getFileHash(file);
            newCommit.addBlob(fileName, fileHash);
            try {
                Files.move(file.toPath(), getBlobFile(fileHash, fileName).toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        writeCommit(newCommit);
        head = newCommit.getHash();
    }

    public static void setupPersistence() {
        GITLET_DIR.mkdir();
        BLOB_DIR.mkdir();
        COMMIT_DIR.mkdir();
        STAGE_DIR.mkdir();
        COMMIT_POINTER_DIR.mkdir();
    }

    public static Commit getCurrentCommit() {
        return Utils.readObject(getCommitFile(head), Commit.class);
    }

    public static void writeCommit(Commit commit) {
        File commitFile = getCommitFile(commit.getHash());
        Utils.writeObject(commitFile, commit);
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
    public static void writeCommitPointers() {
        /* Write head pointer */
        Utils.writeContents(HEAD_FILE, head);
        /* Write branch pointers */
        Utils.writeObject(BRANCHES_FILE, branches);
    }

    public static void readCommitPointers() {
        if (HEAD_FILE.exists()) {
            head = Utils.readContentsAsString(HEAD_FILE);
        }
        if (BRANCHES_FILE.exists()) {
            branches = Utils.readObject(BRANCHES_FILE, TreeMap.class);
        }
    }

}
