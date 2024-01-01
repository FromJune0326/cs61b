package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static gitlet.Utils.*;

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
    // Use branch name as the head pointer
    static String head = null;
    // Store <branch name, commitHash> in tree map
    static TreeMap<String, String> branches = new TreeMap<>();

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    public static final File STAGE_DIR = join(GITLET_DIR, "Stage");

    public static final File REMOVE_DIR = join(STAGE_DIR, "Remove");

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
            head = "master";
            updateCommitPointers(firstCommitHash);
        } else {
            Utils.message("A Gitlet version-control system already exists in the current directory.");
        }
    }

    public static void addFileToStageArea(String fileName){
        Commit curCommit = getCommit(branches.get(head));
        File fileToAdd = Utils.join(CWD, fileName);
        if (!fileToAdd.exists()) {
            Utils.exitWithMsg("File does not exist.");
        }
        File stagedFile = Utils.join(STAGE_DIR, fileName);
        if (curCommit.hasBlob(fileToAdd)) {
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

    public static void removeFile(String fileName) {
        Commit curCommit = getCommit(branches.get(head));
        File file = join(CWD, fileName);
        File stagedFile = join(STAGE_DIR, fileName);
        File stagedToRemoveFile = join(REMOVE_DIR, fileName);
        if (!(stagedFile.exists() || curCommit.hasFile(fileName))) {
            Utils.exitWithMsg("No reason to remove the file");
        }
        if (stagedFile.exists()) {
            Utils.restrictedDelete(stagedFile);
        }
        if (curCommit.hasFile(fileName)) {
            /* Remove the file from working directory and stage for removal */
            Utils.restrictedDelete(file);
            try {
                stagedToRemoveFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void makeNewCommit(String message) {
        List<String> filesToAdd = Utils.plainFilenamesIn(STAGE_DIR);
        List<String> filesToRemove = Utils.plainFilenamesIn(REMOVE_DIR);
        if ((filesToAdd == null || filesToAdd.isEmpty()) &&
                (filesToRemove == null || filesToRemove.isEmpty())) {
            Utils.exitWithMsg("No changes added to the commit.");
        }
        if (message.isEmpty()) {
            Utils.exitWithMsg("Please enter a commit message.");
        }
        Commit curCommit = getCommit(branches.get(head));
        Commit newCommit = curCommit.makeCopy(message);
        /* Update blobs according to staging area */
        if (filesToAdd != null) {
            for (String fileName: filesToAdd) {
                File file = new File(STAGE_DIR, fileName);
                String fileHash = Utils.getFileHash(file);
                newCommit.addBlob(fileName, fileHash);
                try {
                    Files.move(file.toPath(), getBlobFile(fileHash, fileName).toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        if (filesToRemove != null) {
            for (String fileName: filesToRemove) {
                File file = new File(REMOVE_DIR, fileName);
                newCommit.removeBlob(fileName);
                /* Delete staging remove file */
                Utils.restrictedDelete(file);
            }
        }
        writeCommit(newCommit);
        updateCommitPointers(newCommit.getHash());
    }

    public static void printLog() {
        Commit curCommit = getCommit(branches.get(head));
        while (curCommit != null) {
            // TODO: support displaying merge commit
            System.out.println("===");
            System.out.printf("commit %s\n", curCommit.getHash());
            System.out.printf("Date: %s\n", Utils.getFormattedDate(new Date(curCommit.getDateCreated())));
            System.out.println(curCommit.getMessage());
            System.out.println();
            curCommit = getCommit(curCommit.getParentHash());
        }
    }

    public static void printGlobalLog() {

    }

    public static void printCommitByMessage() {

    }

    public static void printStatus() {

    }

    public static void checkoutFile(String commitHash, String fileName) {
        if (commitHash == null) {
            commitHash = branches.get(head);
        } else {
            updateCommitPointers(commitHash);
        }
        Commit commit = getCommit(commitHash);
        if (commit == null) {
            Utils.exitWithMsg("No commit with that id exists");
        }
        if (!commit.hasFile(fileName)) {
            Utils.exitWithMsg("File does not exist in that commit.");
        }
        File workingFile = Utils.join(CWD, fileName);
        File blobFile = getBlobFile(commit.getFileHash(fileName), fileName);
        try {
            Files.copy(blobFile.toPath(), workingFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void checkoutBranch(String branchName) {
        if (!branches.containsKey(branchName)) {
            Utils.exitWithMsg("No such branch exists.");
        }
        if (branchName.equals(head)) {
            Utils.exitWithMsg("No need to checkout the current branch.");
        }
        String curBranch = head;
        head = branchName;
        checkoutCommit(branches.get(curBranch), branches.get(branchName));
    }

    public static void makeBranch(String branchName) {
        if (branches.containsKey(branchName)) {
            Utils.exitWithMsg("A branch with that name already exists.");
        }
        String curCommit = branches.get(head);
        branches.put(branchName, curCommit);
    }

    public static void removeBranch(String branchName) {
        if (!branches.containsKey(branchName)) {
            Utils.exitWithMsg("A branch with that name does not exist.");
        } else if (head.equals(branchName)) {
            Utils.exitWithMsg("Cannot remove the current branch.");
        }
        branches.remove(branchName);
    }

    public static void resetToCommit(String commitHash) {
        File commitFile =  Utils.join(COMMIT_DIR, commitHash.substring(0, 2), commitHash.substring(2));
        String headHash = branches.get(head);
        if (!commitFile.exists() || (!hasAscendentCommit(getCommit(headHash), commitHash) &&
                !hasAscendentCommit(getCommit(commitHash), headHash))) {
            Utils.exitWithMsg("No commit with that id exists.");
        }
        checkoutCommit(branches.get(head), commitHash);
    }

    public static void checkoutCommit(String oldCommitHash, String newCommitHash) {
        Commit curCommit = getCommit(oldCommitHash);
        Commit checkoutCommit = getCommit(newCommitHash);
        List<String> workingFiles = Utils.plainFilenamesIn(CWD);
        if (workingFiles != null) {
            for (String workingFileName: workingFiles) {
                if (!curCommit.hasFile(workingFileName)) {
                    Utils.exitWithMsg("There is an untracked file in the way; delete it, or add and commit it first.");
                }
            }
            // Remove files not tracked in checked-out branch
            for (String workingFileName: workingFiles) {
                if (!checkoutCommit.hasFile(workingFileName)) {
                    File workingFile = Utils.join(CWD, workingFileName);
                    Utils.restrictedDelete(workingFile);
                }
            }
        }

        // Put files from latest commit of checkout branch in working directory
        for (String checkoutBlobName: checkoutCommit.getFiles()) {
            File workingFile = Utils.join(CWD, checkoutBlobName);
            File checkoutBlobFile = getBlobFile(checkoutCommit.getFileHash(checkoutBlobName), checkoutBlobName);
            try {
                Files.copy(checkoutBlobFile.toPath(), workingFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Clear staging area
        File[] filesToAdd = STAGE_DIR.listFiles();
        if (filesToAdd != null) {
            for (File file: filesToAdd) {
                restrictedDelete(file);
            }
        }
        File[] filesToRemove = REMOVE_DIR.listFiles();
        if (filesToRemove != null) {
            for (File file: filesToRemove) {
                restrictedDelete(file);
            }
        }
        updateCommitPointers(newCommitHash);
    }

    /* Merge current branch with target branch */
    public static void mergeBranch(String branchName) {

    }

    public static void setupPersistence() {
        GITLET_DIR.mkdir();
        BLOB_DIR.mkdir();
        COMMIT_DIR.mkdir();
        STAGE_DIR.mkdir();
        REMOVE_DIR.mkdir();
        COMMIT_POINTER_DIR.mkdir();
    }

    public static Commit getCommit(String hash) {
        if (hash != null) {
            return Utils.readObject(getCommitFile(hash), Commit.class);
        } else {
            return null;
        }
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

    public static boolean hasAscendentCommit(Commit commit, String checkCommitHash) {
        if (commit == null) {
            return false;
        } else if (commit.getHash().equals(checkCommitHash)) {
            return true;
        }
        return hasAscendentCommit(getCommit(commit.getParentHash()), checkCommitHash);
    }

    public static void updateCommitPointers(String commitHash) {
        branches.put(head, commitHash);
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
