package gitlet;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TreeMap;

public class BreadFirstPaths {
    private HashSet<String> marked = new HashSet<>();
    private TreeMap<String, Integer> distTo = new TreeMap<>();

    public BreadFirstPaths(Commit commit) {
        Queue<Commit> fringe = new PriorityQueue<>();
        fringe.add(commit);
        marked.add(commit.getHash());
        distTo.put(commit.getHash(), 0);
        while (!fringe.isEmpty()) {
            Commit curCommit = fringe.remove();
            String curHash = curCommit.getHash();
            for (String parentHash: curCommit.getParentHashes()) {
                Commit parent = Commit.getCommit(parentHash);
                if (!marked.contains(parentHash)) {
                    fringe.add(parent);
                    marked.add(parentHash);
                    distTo.put(parentHash, distTo.get(curHash) + 1);
                }
            }
        }
    }

    public boolean hasPathTo(String hash) {
        return marked.contains(hash);
    }

    public TreeMap<String, Integer> getDistMap() {
        return distTo;
    }
}
