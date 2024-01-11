package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

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

    public static final File COMMIT_POINTER_DIR = join(GITLET_DIR, "PTR");
    public static final File HEAD_FILE = Utils.join(COMMIT_POINTER_DIR, "HEAD");
    public static final File BRANCHES_FILE = Utils.join(COMMIT_POINTER_DIR, "BRANCHES");

    /** Init a Gitlet version-control system */
    public static void initCommit() {
        Date initDate = new Date(0);
        Commit firstCommit = new Commit("initial commit", initDate.toString(), null);
        String firstCommitHash = firstCommit.getHash();
        File firstCommitFile = Commit.getCommitFile(firstCommitHash);
        setupPersistence();
        if (!firstCommitFile.exists()) {
            Commit.writeCommit(firstCommit);
            head = "master";
            updateCommitPointers(firstCommitHash);
        } else {
            Utils.message("A Gitlet version-control system already exists in the current directory.");
        }
    }

    public static void stageFileForAdd(File fileToAdd){
        Commit curCommit = Commit.getCommit(branches.get(head));
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
            Utils.copyFileTo(fileToAdd, stagedFile);
            // If staged for removal, remove it from remove dir
            File fileToRemove = Utils.join(REMOVE_DIR, fileToAdd.getName());
            if (fileToRemove.exists()) {
                Utils.restrictedDelete(fileToRemove);
            }
        }
    }

    public static void stageFileForRemove(File fileToRemove) {
        Commit curCommit = Commit.getCommit(branches.get(head));
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
        Commit curCommit = Commit.getCommit(branches.get(head));
        Commit newCommit = curCommit.makeCopy(message);
        /* Update blobs according to staging area */
        if (filesToAdd != null) {
            for (String fileName: filesToAdd) {
                File file = new File(STAGE_DIR, fileName);
                String fileHash = Utils.getFileHash(file);
                newCommit.addBlob(fileName, fileHash);
                Utils.moveFileTo(file, Commit.getBlobFile(fileHash, fileName));
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
        Commit.writeCommit(newCommit);
        updateCommitPointers(newCommit.getHash());
    }

    public static void printCommit(Commit commit) {
        LinkedList<String> parentHashes = commit.getParentHashes();
        System.out.println("===");
        System.out.printf("commit %s\n", commit.getHash());
        if (parentHashes.size() > 1) {
            System.out.printf("Merge: %s %s\n", parentHashes.get(0).substring(0, 7), parentHashes.get(1).substring(0, 7));
        }
        System.out.printf("Date: %s\n", Utils.getFormattedDate(new Date(commit.getDateCreated())));
        System.out.println(commit.getMessage());
        System.out.println();
    }

    public static void printLog() {
        Commit curCommit = Commit.getCommit(branches.get(head));
        while (curCommit != null) {
            printCommit(curCommit);
            LinkedList<String> parentHashes = curCommit.getParentHashes();
            curCommit = null;
            if (!parentHashes.isEmpty()) {
                curCommit = Commit.getCommit(parentHashes.get(0));
            }
        }
    }

    public static void printGlobalLog() {
        List<Commit> commits = getAllCommits();
        for (Commit commit: commits) {
            printCommit(commit);
        }
    }

    public static void printCommitByMessage(String msg) {
        List<Commit> commits = getAllCommits();
        boolean found = false;
        for (Commit commit: commits) {
            if (commit.getMessage().contains(msg)) {
                System.out.println(commit.getHash());
                found = true;
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
        }
    }

    public static void printStatus() {
        // Print branches
        System.out.println("=== Branches ===");
        List<String> branchNames = new ArrayList<String>(branches.keySet());
        Collections.sort(branchNames);
        for (String branchName: branchNames) {
            if (head.equals(branchName)) {
                System.out.print("*");
            }
            System.out.println(branchName);
        }
        System.out.println();

        // Print staged files
        System.out.println("=== Staged Files ===");
        List<String> filesToAdd = Utils.plainFilenamesIn(STAGE_DIR);
        if (filesToAdd != null) {
            Collections.sort(filesToAdd);
            for (String file: filesToAdd) {
                System.out.println(file);
            }
        }
        System.out.println();

        // Print removed files
        System.out.println("=== Removed Files ===");
        List<String> filesToRemove = Utils.plainFilenamesIn(REMOVE_DIR);
        if (filesToRemove != null) {
            Collections.sort(filesToRemove);
            for (String file: filesToRemove) {
                System.out.println(file);
            }
        }
        System.out.println();

        // Print modified but not staged files
        Commit curCommit = Commit.getCommit(branches.get(head));
        List<String> workingFiles = Utils.plainFilenamesIn(CWD);
        List<String> commitedFiles = new ArrayList<>(curCommit.getFiles());
        List<String> untrackedFiles = new ArrayList<>();
        List<String> modifiedFiles = new ArrayList<>();
        List<String> deletedFiles = new ArrayList<>();

        if (workingFiles != null) {
            Collections.sort(workingFiles);
            for (String workingFile: workingFiles) {
                if (!commitedFiles.contains(workingFile)) {
                    if (filesToAdd.contains(workingFile)) {
                        //TODO: implement this later
                    }
                }
            }
        }
        System.out.println("=== Modifications Not Staged For Commit ===");

        // print untracked files
        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();
    }

    public static void checkoutFile(String commitHash, String fileName) {
        if (commitHash == null) {
            commitHash = branches.get(head);
        } else {
            updateCommitPointers(commitHash);
        }
        Commit commit = Commit.getCommit(commitHash);
        if (commit == null) {
            Utils.exitWithMsg("No commit with that id exists");
        }
        if (!commit.hasFile(fileName)) {
            Utils.exitWithMsg("File does not exist in that commit.");
        }
        File workingFile = Utils.join(CWD, fileName);
        File blobFile = Commit.getBlobFile(commit.getFileHash(fileName), fileName);
        Utils.copyFileTo(blobFile, workingFile);
    }

    public static void checkoutBranch(String branchName) {
        if (!branches.containsKey(branchName)) {
            Utils.exitWithMsg("No such branch exists.");
        }
        if (branchName.equals(head)) {
            Utils.exitWithMsg("No need to checkout the current branch.");
        }
        checkUntrackedFiles();
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
        File commitFile =  Utils.join(Commit.COMMIT_DIR, commitHash.substring(0, 2), commitHash.substring(2));
        String headHash = branches.get(head);
        if (!commitFile.exists() || (!hasAscendentCommit(Commit.getCommit(headHash), commitHash) &&
                !hasAscendentCommit(Commit.getCommit(commitHash), headHash))) {
            Utils.exitWithMsg("No commit with that id exists.");
        }
        checkUntrackedFiles();
        checkoutCommit(branches.get(head), commitHash);
    }

    public static void checkoutCommit(String oldCommitHash, String newCommitHash) {
        Commit curCommit = Commit.getCommit(oldCommitHash);
        Commit checkedoutCommit = Commit.getCommit(newCommitHash);
        List<String> workingFiles = Utils.plainFilenamesIn(CWD);
        if (workingFiles != null) {
            // Remove files not tracked in checked-out branch
            for (String workingFileName: workingFiles) {
                if (!checkedoutCommit.hasFile(workingFileName)) {
                    File workingFile = Utils.join(CWD, workingFileName);
                    Utils.restrictedDelete(workingFile);
                }
            }
        }

        // Put files from latest commit of checkout branch in working directory
        for (String checkoutBlobName: checkedoutCommit.getFiles()) {
            File workingFile = Utils.join(CWD, checkoutBlobName);
            File checkoutBlobFile = Commit.getBlobFile(checkedoutCommit.getFileHash(checkoutBlobName), checkoutBlobName);
            Utils.copyFileTo(checkoutBlobFile, workingFile);
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

        Commit curCommit = Commit.getCommit(branches.get(head));
        Commit otherCommit = Commit.getCommit(branches.get(branchName));
        Commit splitPoint = getLastSplitPoint(curCommit, otherCommit);
        if (otherCommit.getHash().equals(splitPoint.getHash())) {
            Utils.exitWithMsg("Given branch is an ancestor of the current branch.");
        }
        if (curCommit.getHash().equals(splitPoint.getHash())) {
            checkoutBranch(branchName);
            Utils.exitWithMsg("Current branch fast-forwarded.");
        }
        Commit mergeCommit = curCommit.makeCopy(String.format("Merged %s into %s.", branchName, head));
        mergeCommit.addParentHash(otherCommit.getHash());
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
                orgFile = Commit.getBlobFile(orgFileHash, fileName);
                orgFileContent = Utils.readContentsAsString(orgFile);
            }
            if (curFileHash != null) {
                curFile = Commit.getBlobFile(curFileHash,  fileName);
                curFileContent = Utils.readContentsAsString(curFile);
            }
            if (otherFileHash != null) {
                otherFile = Commit.getBlobFile(otherFileHash, fileName);
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
                        Utils.restrictedDelete(Utils.join(CWD, fileName));
                    }
                }
                // Unmodified in other but not present in HEAD, remain removed
            } else {
                // Not in split nor other but in HEAD, leave it unchanged
                if (curFileHash == null && otherFileHash != null) {
                    // Not in split nor HEAD but in other, stage it
                    Utils.copyFileTo(otherFile, Utils.join(CWD, fileName));
                    stageFileForAdd(otherFile);
                }
            }
        }
        // Make merge commit
        Commit.writeCommit(mergeCommit);
        updateCommitPointers(mergeCommit.getHash());
    }

    public static void setupPersistence() {
        GITLET_DIR.mkdir();
        Commit.BLOB_DIR.mkdir();
        Commit.COMMIT_DIR.mkdir();
        STAGE_DIR.mkdir();
        REMOVE_DIR.mkdir();
        COMMIT_POINTER_DIR.mkdir();
    }


    public static boolean hasStagedFiles() {
        List<String> filesToAdd = Utils.plainFilenamesIn(STAGE_DIR);
        List<String> filesToRemove = Utils.plainFilenamesIn(REMOVE_DIR);
        return (filesToAdd != null && !filesToAdd.isEmpty()) ||
                (filesToRemove != null && !filesToRemove.isEmpty());
    }

    public static void checkUntrackedFiles() {
        Commit curCommit = Commit.getCommit(branches.get(head));
        List<String> workingFiles = Utils.plainFilenamesIn(CWD);
        if (workingFiles != null) {
            for (String workingFileName: workingFiles) {
                if (!curCommit.hasFile(workingFileName)) {
                    Utils.exitWithMsg("There is an untracked file in the way; delete it, or add and commit it first.");
                }
            }
        }
    }

    public static void checkInitRepo() {
        if (!GITLET_DIR.exists()) {
            Utils.exitWithMsg("Not in an initialized Gitlet directory.");
        }
    }

    public static boolean hasAscendentCommit(Commit commit, String checkCommitHash) {
        BreadFirstPaths depthMap = (new BreadFirstPaths(commit));
        return depthMap.hasPathTo(checkCommitHash);
    }

    public static Commit getLastSplitPoint(Commit commit1, Commit commit2) {
        TreeMap<String, Integer> branch1DepthMap = (new BreadFirstPaths(commit1)).getDistMap();
        TreeMap<String, Integer> branch2DepthMap = (new BreadFirstPaths(commit2)).getDistMap();
        String splitPoint = null;
        Integer minDepth = Integer.MAX_VALUE;
        for (String commitHash: branch1DepthMap.keySet()) {
            if (branch2DepthMap.containsKey(commitHash) && branch2DepthMap.get(commitHash) < minDepth) {
                minDepth = branch2DepthMap.get(commitHash);
                splitPoint = commitHash;
            }
        }
        return Commit.getCommit(splitPoint);
    }

    public static List<Commit> getAllCommits() {
        List<File> commitFiles = Utils.filesIn(Commit.COMMIT_DIR);
        List<Commit> commits = new ArrayList<>();
        for (File file: commitFiles) {
            commits.add(Commit.getCommit(file));
        }
        return commits;
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
