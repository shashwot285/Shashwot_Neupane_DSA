import java.util.*;

// Question 1a: Maximum Points on a Line
// Given customer locations as [x,y] coordinates, find the maximum number
// of homes that lie on the same straight line (signal repeater placement).
// Algorithm: For each point, hash slopes as reduced fractions to avoid
//            floating-point errors. O(n^2) time, O(n) space.
public class MaxPointsOnLine {

    static int gcd(int a, int b) {
        a = Math.abs(a); b = Math.abs(b);
        while (b != 0) { int t = b; b = a % b; a = t; }
        return a;
    }

    static int maxPoints(int[][] points) {
        int n = points.length;
        if (n <= 2) return n;
        int max = 1;

        for (int i = 0; i < n; i++) {
            Map<String, Integer> slopeMap = new HashMap<>();
            int dup = 1;

            for (int j = i + 1; j < n; j++) {
                int dx = points[j][0] - points[i][0];
                int dy = points[j][1] - points[i][1];

                if (dx == 0 && dy == 0) { dup++; continue; }

                int g = gcd(Math.abs(dx), Math.abs(dy));
                dx /= g; dy /= g;

                if (dx < 0) { dx = -dx; dy = -dy; }
                else if (dx == 0) { dy = Math.abs(dy); }

                String key = dx + "," + dy;
                slopeMap.put(key, slopeMap.getOrDefault(key, 0) + 1);
            }

            int localMax = dup;
            for (int cnt : slopeMap.values())
                localMax = Math.max(localMax, cnt + dup);
            max = Math.max(max, localMax);
        }
        return max;
    }

    public static void main(String[] args) {
        // Example 1: all three on diagonal
        int[][] loc1 = {{1,1},{2,2},{3,3}};
        System.out.println("Example 1: " + maxPoints(loc1)); // Expected: 3

        // Example 2: four points on one line
        int[][] loc2 = {{1,1},{3,2},{5,3},{4,1},{2,3},{1,4}};
        System.out.println("Example 2: " + maxPoints(loc2)); // Expected: 4

        // Edge: duplicates
        int[][] loc3 = {{1,1},{1,1},{2,2}};
        System.out.println("Duplicates: " + maxPoints(loc3)); // Expected: 3
    }
}