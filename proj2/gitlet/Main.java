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
        Repository.readCommitPointers();
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                Repository.initCommit();
                break;
            case "add":
                validateNumArgs("add", args, 2);
                Repository.addFileToStageArea(args[1]);
                break;
            case "commit":
                validateNumArgs("commit", args, 2);
                Repository.makeNewCommit(args[1]);
                break;
            case "rm":
                validateNumArgs("rm", args, 2);
                Repository.removeFile(args[1]);
                break;
            case "log":
                validateNumArgs("log", args, 1);
                Repository.printLog();
                break;
            case "global-log":
                validateNumArgs("global-log", args, 1);
                // TODO: implement after supporting branch
                break;
            case "find":
                validateNumArgs("find", args, 2);
                break;
            case "status":
                break;
            case "checkout":
                if (args.length == 2) {
                    String branchName = args[1];
                    // Checkout branch
                    // eg: checkout [branch name]
                } else if (args.length == 3) {
                    // Checkout head file
                    // eg: checkout -- [file name]
                    Repository.checkoutFile(null, args[2]);
                } else if (args.length == 4) {
                    // Checkout specific commit file
                    // eg: checkout [commit id] -- [file name]
                    Repository.checkoutFile(args[1], args[3]);
                }
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
}
