import java.util.*;

// Question 2: Maximum Path Sum in Binary Tree (Hydropower Cascade)
// Find the maximum total power from any continuous sequence of connected plants.
// The path can start and end at any node — it does not need to pass through root.
// Algorithm: Post-order DFS, track global max. O(n) time, O(h) space.
public class MaxPathSumBinaryTree {

    static class TreeNode {
        int val;
        TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    static int maxPower;

    static int maxPathSum(TreeNode root) {
        maxPower = Integer.MIN_VALUE;
        dfs(root);
        return maxPower;
    }

    // Returns best single-arm gain downward from this node.
    static int dfs(TreeNode node) {
        if (node == null) return 0;
        int left  = Math.max(dfs(node.left),  0); // ignore negative branches
        int right = Math.max(dfs(node.right), 0);
        maxPower  = Math.max(maxPower, node.val + left + right); // path through this node
        return node.val + Math.max(left, right);                 // best arm for parent
    }

    // Build tree from level-order array (null = missing node)
    static TreeNode build(Integer[] vals) {
        if (vals == null || vals.length == 0 || vals[0] == null) return null;
        TreeNode root = new TreeNode(vals[0]);
        Queue<TreeNode> q = new LinkedList<>();
        q.offer(root);
        int i = 1;
        while (!q.isEmpty() && i < vals.length) {
            TreeNode cur = q.poll();
            if (i < vals.length && vals[i] != null) { cur.left  = new TreeNode(vals[i]); q.offer(cur.left);  } i++;
            if (i < vals.length && vals[i] != null) { cur.right = new TreeNode(vals[i]); q.offer(cur.right); } i++;
        }
        return root;
    }

    public static void main(String[] args) {
        // Example 1: root=[1,2,3]  path: 2->1->3 = 6
        System.out.println("Example 1: " + maxPathSum(build(new Integer[]{1, 2, 3}))); // 6

        // Example 2: root=[-10,9,20,null,null,15,7]  path: 15->20->7 = 42
        System.out.println("Example 2: " + maxPathSum(build(new Integer[]{-10, 9, 20, null, null, 15, 7}))); // 42

        // Edge: single negative node
        System.out.println("Single -3: " + maxPathSum(build(new Integer[]{-3}))); // -3

        // Edge: all negative
        System.out.println("All neg [-1,-2,-3]: " + maxPathSum(build(new Integer[]{-1,-2,-3}))); // -1
    }
}