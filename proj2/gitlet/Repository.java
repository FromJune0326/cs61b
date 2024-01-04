package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
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

    public static void stageFileForAdd(File fileToAdd){
        Commit curCommit = getCommit(branches.get(head));
        if (!fileToAdd.exists()) {
            Utils.exitWithMsg("File does not exist.");
        }
        File stagedFile = Utils.join(STAGE_DIR, fileToAdd.getName());
        if (curCommit.hasBlob(fileToAdd)) {
            if (stagedFile.exists()) {
                // Removed staged file if the file to add is the same as committed one
                Utils.restrictedDelete(stagedFile);
            }
        } else {
            try {
                Files.copy(fileToAdd.toPath(), stagedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // If staged for removal, remove it from remove dir
            File fileToRemove = Utils.join(REMOVE_DIR, fileToAdd.getName());
            if (fileToRemove.exists()) {
                Utils.restrictedDelete(fileToRemove);
            }
        }
    }

    public static void stageFileForRemove(File fileToRemove) {
        Commit curCommit = getCommit(branches.get(head));
        String fileName = fileToRemove.getName();
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
            if (fileToRemove.exists()) {
                Utils.restrictedDelete(fileToRemove);
            }
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
            checkUntrackedFiles();
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
        checkUntrackedFiles();
        if (hasStagedFiles()) {
            Utils.exitWithMsg("You have uncommitted changes.");
        }
        if (!branches.containsKey(branchName)) {
            Utils.exitWithMsg("A branch with that name does not exist.");
        }
        if (branchName.equals(head)) {
            Utils.exitWithMsg("Cannot merge a branch with itself.");
        }

        Commit curCommit = getCommit(branches.get(head));
        Commit otherCommit = getCommit(branches.get(branchName));
        Commit splitPoint = getLastSplitPoint(curCommit, otherCommit);
        if (otherCommit.getHash().equals(splitPoint.getHash())) {
            Utils.exitWithMsg("Given branch is an ancestor of the current branch.");
        }
        if (curCommit.getHash().equals(splitPoint.getHash())) {
            checkoutBranch(branchName);
            Utils.exitWithMsg("Current branch fast-forwarded.");
        }
        Commit mergeCommit = curCommit.makeCopy(String.format("Merge %s into %s", branchName, head));
        Set<String> allFiles = new HashSet<>();
        allFiles.addAll(curCommit.getFiles());
        allFiles.addAll(otherCommit.getFiles());
        allFiles.addAll(splitPoint.getFiles());
        for (String fileName: allFiles) {
            String orgFileHash = splitPoint.getFileHash(fileName);
            String curFileHash = curCommit.getFileHash(fileName);
            String otherFileHash = otherCommit.getFileHash(fileName);
            File orgFile = null;
            File curFile = null;
            File otherFile = null;
            String orgFileContent = null;
            String curFileContent = null;
            String otherFileContent = null;
            if (orgFileHash != null) {
                orgFile = getBlobFile(orgFileHash, fileName);
                orgFileContent = Utils.readContentsAsString(orgFile);
            }
            if (curFileHash != null) {
                curFile = getBlobFile(curFileHash,  fileName);
                curFileContent = Utils.readContentsAsString(curFile);
            }
            if (otherFileHash != null) {
                otherFile = getBlobFile(otherFileHash, fileName);
                otherFileContent = Utils.readContentsAsString(otherFile);
            }

            if (orgFileHash != null) {
                if (curFileHash != null && otherFileHash != null) {
                    if (curFileContent.equals(orgFileContent) && !otherFileContent.equals(orgFileContent)) {
                        // Modified in other but not HEAD, stage it
                        stageFileForAdd(otherFile);
                    } else if (!curFileContent.equals(orgFileContent) && !otherFileContent.equals(orgFileContent)) {
                        // Modified in both other and HEAD in different way,
                        // put them into one file then stage. Print encounter merge conflict
                        String mergeFileContent = "<<<<<<< HEAD\n";
                        mergeFileContent += curFileContent;
                        mergeFileContent += "=======\n";
                        mergeFileContent += otherFileContent;
                        mergeFileContent += ">>>>>>>\n";
                        File mergeFile = Utils.join(STAGE_DIR, fileName);
                        writeContents(mergeFile, mergeFileContent);
                        System.out.print("Encountered a merge conflict.");
                    }
                    // Modified in HEAD but not in other, leave it unchanged
                    // Modified in both other and HEAD in same way, leave it unchanged.
                } else if (curFileHash != null) {
                    // Unmodified in HEAD but not present in other, remove it
                    if (curFileContent.equals(orgFileContent)) {
                        mergeCommit.removeBlob(fileName);
                    }
                }
                // Unmodified in other but not present in HEAD, remain removed
            } else {
                // Not in split nor other but in HEAD, leave it unchanged
                if (curFileHash == null && otherFileHash != null) {
                    // Not in split nor HEAD but in other, stage it
                    stageFileForAdd(otherFile);
                }
            }
        }
        // Make merge commit
        writeCommit(mergeCommit);
        updateCommitPointers(mergeCommit.getHash());
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

    public static boolean hasStagedFiles() {
        List<String> filesToAdd = Utils.plainFilenamesIn(STAGE_DIR);
        List<String> filesToRemove = Utils.plainFilenamesIn(REMOVE_DIR);
        return (filesToAdd != null && !filesToAdd.isEmpty()) ||
                (filesToRemove != null && !filesToRemove.isEmpty());
    }

    public static void checkUntrackedFiles() {
        Commit curCommit = getCommit(branches.get(head));
        List<String> workingFiles = Utils.plainFilenamesIn(CWD);
        if (workingFiles != null) {
            for (String workingFileName: workingFiles) {
                if (!curCommit.hasFile(workingFileName)) {
                    Utils.exitWithMsg("There is an untracked file in the way; delete it, or add and commit it first.");
                }
            }
        }
    }

    public static boolean hasAscendentCommit(Commit commit, String checkCommitHash) {
        if (commit == null) {
            return false;
        } else if (commit.getHash().equals(checkCommitHash)) {
            return true;
        }
        return false;
//        for (String parentHash: commit.getParentHashes()) {
//            return hasAscendentCommit(getCommit(parentHash), checkCommitHash);
//        }
    }

    public static Commit getLastSplitPoint(Commit commit1, Commit commit2) {
        Set<String> branch1Hashes = new HashSet<>();
        Set<String> branch2Hashes = new HashSet<>();
        return _getSplitPoint(0, commit1, commit2, branch1Hashes, branch2Hashes);
    }

    private static Commit _getSplitPoint(int count, Commit commit1, Commit commit2, Set<String> branch1Hashes, Set<String>branch2Hashes) {
        if (commit1 == null && commit2 == null) {
            return null;
        }
        if (count % 2 == 0 && commit1 != null) {
            branch1Hashes.add(commit1.getHash());
            if (branch1Hashes.contains(commit1.getHash()) && branch2Hashes.contains(commit1.getHash())) {
                return commit1;
            } else {
                commit1 = getCommit(commit1.getParentHash());
            }
        } else {
            branch2Hashes.add(commit2.getHash());
            if (branch1Hashes.contains(commit2.getHash()) && branch2Hashes.contains(commit2.getHash())) {
                return commit2;
            } else {
                commit2 = getCommit(commit2.getParentHash());
            }
        }
        return _getSplitPoint(count + 1, commit1, commit2, branch1Hashes, branch2Hashes);
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
