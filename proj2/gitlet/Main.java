package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author yyy
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            Utils.exitWithMsg("Please enter a command.");
        }
        String firstArg = args[0];
        if (!firstArg.equals("init")) {
            Repository.checkInitRepo();
        }
        Repository.readCommitPointers();
        switch(firstArg) {
            case "init":
                Repository.initCommit();
                break;
            case "add":
                validateNumArgs("add", args, 2);
                Repository.stageFileForAdd(Utils.join(Repository.CWD, args[1]));
                break;
            case "commit":
                validateNumArgs("commit", args, 2);
                Repository.writeNewCommit(args[1]);
                break;
            case "rm":
                validateNumArgs("rm", args, 2);
                Repository.stageFileForRemove(Utils.join(Repository.CWD, args[1]));
                break;
            case "log":
                validateNumArgs("log", args, 1);
                Repository.printLog();
                break;
            case "global-log":
                validateNumArgs("global-log", args, 1);
                Repository.printGlobalLog();
                break;
            case "find":
                validateNumArgs("find", args, 2);
                Repository.printCommitByMessage(args[1]);
                break;
            case "status":
                validateNumArgs("status", args, 1);
                Repository.printStatus();
                break;
            case "checkout":
                if (args.length == 2) {
                    // Checkout branch
                    // eg: checkout [branch name]
                    String branchName = args[1];
                    Repository.checkoutBranch(branchName);
                } else if (args.length == 3) {
                    // Checkout head file
                    // eg: checkout -- [file name]
                    validateOperand(args[1], "--");
                    Repository.checkoutFile(null, args[2]);
                } else if (args.length == 4) {
                    // Checkout specific commit file
                    // eg: checkout [commit id] -- [file name]
                    validateOperand(args[2], "--");
                    Repository.checkoutFile(args[1], args[3]);
                }
                break;
            case "branch":
                validateNumArgs("branch", args, 2);
                Repository.makeBranch(args[1]);
                break;
            case "rm-branch":
                validateNumArgs("branch", args, 2);
                Repository.removeBranch(args[1]);
                break;
            case "reset":
                validateNumArgs("reset", args, 2);
                Repository.resetToCommit(args[1]);
                break;
            case "merge":
                validateNumArgs("merge", args, 2);
                Repository.mergeBranch(args[1]);
                break;
            default:
                Utils.exitWithMsg("No command with that name exists.");
        }
        Repository.writeCommitPointers();
        return;
    }

    /**
     * Checks the number of arguments versus the expected number,
     * throws a RuntimeException if they do not match.
     *
     * @param cmd Name of command you are validating
     * @param args Argument array from command line
     * @param n Number of expected arguments
     */
    public static void validateNumArgs(String cmd, String[] args, int n) {
        if (args.length != n) {
            throw new RuntimeException(
                    String.format("Invalid number of arguments for: %s.", cmd));
        }
    }

    public static void validateOperand(String operand, String check) {
        if (!operand.equals(check)) {
            Utils.exitWithMsg("Incorrect operands.");
        }
    }
}
