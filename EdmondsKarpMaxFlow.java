import java.util.*;

// Question 6 Part 3: Maximum Throughput — Edmonds-Karp (BFS Ford-Fulkerson)
// Find the maximum number of supply trucks that can travel from KTM to BS
// simultaneously. Models the road network as a directed flow graph.
//
// Algorithm: Edmonds-Karp uses BFS to find the shortest augmenting path in the
//            residual graph at each iteration, guaranteeing O(VE^2) time.
// Min-Cut: After max flow, BFS from source on residual graph identifies S.
//          Cut edges (S->T) capacity = max flow, proving Max-Flow Min-Cut theorem.
public class EdmondsKarpMaxFlow {

    static class Edge {
        int to, rev, cap;
        Edge(int to, int rev, int cap) { this.to = to; this.rev = rev; this.cap = cap; }
    }

    static class MaxFlow {
        List<Edge>[] g;

        @SuppressWarnings("unchecked")
        MaxFlow(int n) {
            g = new ArrayList[n];
            for (int i = 0; i < n; i++) g[i] = new ArrayList<>();
        }

        void addEdge(int u, int v, int cap) {
            g[u].add(new Edge(v, g[v].size(), cap));  // forward edge
            g[v].add(new Edge(u, g[u].size() - 1, 0)); // reverse edge (residual)
        }

        int maxFlow(int s, int t) {
            int flow = 0;
            int n = g.length;

            while (true) {
                // BFS to find shortest augmenting path in residual graph
                int[] parentV = new int[n];
                int[] parentE = new int[n];
                Arrays.fill(parentV, -1);
                Queue<Integer> q = new ArrayDeque<>();
                q.add(s);
                parentV[s] = s;

                while (!q.isEmpty() && parentV[t] == -1) {
                    int u = q.poll();
                    for (int i = 0; i < g[u].size(); i++) {
                        Edge e = g[u].get(i);
                        if (parentV[e.to] == -1 && e.cap > 0) {
                            parentV[e.to] = u;
                            parentE[e.to] = i;
                            q.add(e.to);
                        }
                    }
                }

                if (parentV[t] == -1) break; // no augmenting path — done

                // Find bottleneck capacity along the path
                int add = Integer.MAX_VALUE;
                for (int v = t; v != s; v = parentV[v]) {
                    Edge e = g[parentV[v]].get(parentE[v]);
                    add = Math.min(add, e.cap);
                }

                // Augment flow: reduce forward, increase reverse
                for (int v = t; v != s; v = parentV[v]) {
                    Edge e = g[parentV[v]].get(parentE[v]);
                    e.cap -= add;
                    g[v].get(e.rev).cap += add;
                }
                flow += add;
            }
            return flow;
        }

        // Find nodes reachable from s in residual graph (for min-cut)
        boolean[] reachable(int s) {
            int n = g.length;
            boolean[] vis = new boolean[n];
            Queue<Integer> q = new ArrayDeque<>();
            q.add(s); vis[s] = true;
            while (!q.isEmpty()) {
                int u = q.poll();
                for (Edge e : g[u])
                    if (!vis[e.to] && e.cap > 0) { vis[e.to] = true; q.add(e.to); }
            }
            return vis;
        }
    }

    public static void main(String[] args) {
        // Nodes: 0=KTM, 1=JA, 2=JB, 3=PH, 4=BS
        MaxFlow mf = new MaxFlow(5);
        mf.addEdge(0, 1, 10); // KTM -> JA
        mf.addEdge(0, 2, 15); // KTM -> JB
        mf.addEdge(1, 0, 10); // JA  -> KTM
        mf.addEdge(1, 3,  8); // JA  -> PH
        mf.addEdge(1, 4,  5); // JA  -> BS
        mf.addEdge(2, 0, 15); // JB  -> KTM
        mf.addEdge(2, 1,  4); // JB  -> JA
        mf.addEdge(2, 4, 12); // JB  -> BS
        mf.addEdge(3, 1,  8); // PH  -> JA
        mf.addEdge(3, 4,  6); // PH  -> BS
        mf.addEdge(4, 1,  5); // BS  -> JA
        mf.addEdge(4, 2, 12); // BS  -> JB
        mf.addEdge(4, 3,  6); // BS  -> PH

        int source = 0, sink = 4;
        int maxFlowVal = mf.maxFlow(source, sink);
        System.out.println("Max flow (KTM -> BS): " + maxFlowVal); // Expected: 23

        // Min S-T Cut verification (Max-Flow Min-Cut theorem)
        boolean[] S = mf.reachable(source);
        String[] names = {"KTM","JA","JB","PH","BS"};

        System.out.print("S (reachable from KTM): ");
        for (int i = 0; i < 5; i++) if (S[i]) System.out.print(names[i] + " ");
        System.out.print("\nT (sink side): ");
        for (int i = 0; i < 5; i++) if (!S[i]) System.out.print(names[i] + " ");
        System.out.println();

        // Original capacities for cut calculation
        int[][] origCap = {
            {0,10,15,0,0},{10,0,0,8,5},{15,4,0,0,12},{0,8,0,0,6},{0,5,12,6,0}
        };
        int cutCap = 0;
        System.out.println("Cut edges:");
        for (int u = 0; u < 5; u++)
            for (int v = 0; v < 5; v++)
                if (S[u] && !S[v] && origCap[u][v] > 0) {
                    System.out.printf("  %s -> %s  cap=%d%n", names[u], names[v], origCap[u][v]);
                    cutCap += origCap[u][v];
                }
        System.out.println("Min cut capacity: " + cutCap);
        System.out.println("Max-Flow == Min-Cut: " + (maxFlowVal == cutCap)); // true
    }
}