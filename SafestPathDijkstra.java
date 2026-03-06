import java.util.*;

// Question 6 Part 1 & 2: Safest Path — Modified Dijkstra with Log Transform
//
// Problem: Find the path with maximum PRODUCT of safety probabilities from KTM
//          to each relief center (PH and BS).
//
// Why standard Dijkstra fails:
//   (a) Standard Dijkstra minimises SUM of weights. Path safety is a PRODUCT
//       of probabilities — treating them as distances gives meaningless results.
//   (b) Directly maximising probability products breaks Dijkstra's RELAX step
//       because multiplying values in (0,1] always decreases them; greedy
//       "pick highest neighbour" does not guarantee the globally safest path.
//
// Transformation: w'(e) = -ln(p(e))
//   Maximise  Π p(e)  ≡  Minimise  Σ -ln(p(e))
//   because   ln(Π p) = Σ ln(p)  and ln is monotonically increasing.
//   All w'(e) >= 0 since p ∈ (0,1] → -ln(p) >= 0.  Dijkstra applies. ✓
//
// Modified RELAX:  if (dist[v] > dist[u] + (-ln(p(u,v)))) → update dist[v]
//
// Correctness: Minimising Σ -ln(p) = Maximising Σ ln(p) = Maximising Π p. ∎
public class SafestPathDijkstra {

    static final String[] NODES = {"KTM","JA","JB","PH","BS"};

    static final double[][] EDGES = {
        // {fromIdx, toIdx, probability}
        {0,1,0.90},{0,2,0.80},
        {1,0,0.90},{1,3,0.95},{1,4,0.70},
        {2,0,0.80},{2,1,0.60},{2,4,0.90},
        {3,1,0.95},{3,4,0.85},
        {4,1,0.70},{4,2,0.90},{4,3,0.85}
    };

    static int idx(String n) {
        for (int i = 0; i < NODES.length; i++) if (NODES[i].equals(n)) return i;
        return -1;
    }

    public static void main(String[] args) {
        int N = NODES.length;
        int src = idx("KTM");

        // Build adjacency list
        List<List<double[]>> adj = new ArrayList<>();
        for (int i = 0; i < N; i++) adj.add(new ArrayList<>());
        for (double[] e : EDGES) adj.get((int)e[0]).add(new double[]{e[1], e[2]});

        // Modified Dijkstra with w'(e) = -ln(p(e))
        double[] dist = new double[N];
        int[]    prev = new int[N];
        Arrays.fill(dist, Double.MAX_VALUE);
        Arrays.fill(prev, -1);
        dist[src] = 0.0;

        // Min-heap on transformed cost
        PriorityQueue<double[]> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> a[0]));
        pq.offer(new double[]{0.0, src});

        while (!pq.isEmpty()) {
            double[] top = pq.poll();
            double cost = top[0];
            int u = (int) top[1];
            if (cost > dist[u]) continue; // stale entry

            for (double[] edge : adj.get(u)) {
                int    v       = (int) edge[0];
                double p       = edge[1];
                double newCost = dist[u] + (-Math.log(p)); // RELAX with log transform
                if (newCost < dist[v]) {
                    dist[v] = newCost;
                    prev[v] = u;
                    pq.offer(new double[]{newCost, v});
                }
            }
        }

        System.out.println("Safest paths from KTM:");
        System.out.printf("%-12s %-35s %-12s%n", "Destination", "Path", "Safety Prob");
        System.out.println("-".repeat(60));

        for (String dest : new String[]{"JA","JB","PH","BS"}) {
            int d = idx(dest);
            double safety = dist[d] == Double.MAX_VALUE ? 0 : Math.exp(-dist[d]);

            // Reconstruct path
            List<String> path = new ArrayList<>();
            int cur = d;
            while (cur != -1) { path.add(0, NODES[cur]); cur = prev[cur]; }
            String pathStr = path.get(0).equals("KTM") ? String.join(" -> ", path) : "No path";

            System.out.printf("%-12s %-35s %.4f%n", dest, pathStr, safety);
        }
    }
}