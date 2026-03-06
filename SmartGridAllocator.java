import java.util.*;

// Question 4: Smart Energy Grid Load Distribution Optimization (Nepal)
// Allocate energy from Solar, Hydro, and Diesel sources to 3 districts each hour.
// Algorithm: Greedy — always pick cheapest available source first.
//            ±10% demand flexibility to avoid unnecessary diesel use.
// Time: O(H * S * D) where H=18 hours, S=3 sources, D=3 districts — near constant.
public class SmartGridAllocator {

    static class Source {
        String id, type;
        double cap, cost;
        int start, end;
        Source(String id, String type, double cap, int start, int end, double cost) {
            this.id = id; this.type = type; this.cap = cap;
            this.start = start; this.end = end; this.cost = cost;
        }
    }

    static final String[] DISTRICTS = {"A", "B", "C"};
    static final double   TOLERANCE = 0.10;

    // [hour, demandA, demandB, demandC]
    static final int[][] DEMAND = {
        { 6,20,15,25},{ 7,22,16,28},{ 8,25,18,30},{ 9,28,20,32},
        {10,30,22,35},{11,32,24,38},{12,35,26,40},{13,33,25,38},
        {14,30,23,35},{15,28,21,33},{16,25,19,30},{17,28,22,35},
        {18,30,25,38},{19,32,27,40},{20,35,28,42},{21,30,25,38},
        {22,25,20,32},{23,18,14,22}
    };

    static final Source[] SOURCES = {
        new Source("S1","Solar", 50,  6, 18, 1.0),
        new Source("S2","Hydro", 40,  0, 24, 1.5),
        new Source("S3","Diesel",60, 17, 23, 3.0)
    };

    public static void main(String[] args) {
        System.out.printf("%-6s %-6s %7s %7s %8s %7s %8s %7s%n",
            "Hour","Dist","Solar","Hydro","Diesel","Total","Demand","% Met");
        System.out.println("-".repeat(65));

        double totalCost = 0, totalRenew = 0, totalDiesel = 0;
        List<Integer> dieselHours = new ArrayList<>();

        for (int[] row : DEMAND) {
            int hour = row[0];
            double[] demand = {row[1], row[2], row[3]};

            // Get available sources sorted cheapest first (Greedy)
            List<Source> avail = new ArrayList<>();
            for (Source s : SOURCES)
                if (s.start <= hour && hour < s.end) avail.add(s);
            avail.sort(Comparator.comparingDouble(s -> s.cost));

            double[] remCap  = new double[avail.size()];
            for (int si = 0; si < avail.size(); si++) remCap[si] = avail.get(si).cap;

            double[][] alloc = new double[3][avail.size()];
            double[]   remD  = Arrays.copyOf(demand, 3);

            // Fill cheapest source first across all districts
            for (int si = 0; si < avail.size(); si++) {
                for (int d = 0; d < 3; d++) {
                    if (remD[d] <= 0 || remCap[si] <= 0) continue;
                    double give = Math.min(remD[d], remCap[si]);
                    alloc[d][si] += give;
                    remD[d]      -= give;
                    remCap[si]   -= give;
                }
            }

            // Apply ±10% tolerance
            for (int d = 0; d < 3; d++)
                if (remD[d] > 0 && remD[d] <= demand[d] * TOLERANCE) remD[d] = 0;

            boolean usedDiesel = false;
            for (int d = 0; d < 3; d++) {
                double solar = 0, hydro = 0, diesel = 0;
                for (int si = 0; si < avail.size(); si++) {
                    if (avail.get(si).type.equals("Solar"))  solar  += alloc[d][si];
                    if (avail.get(si).type.equals("Hydro"))  hydro  += alloc[d][si];
                    if (avail.get(si).type.equals("Diesel")) diesel += alloc[d][si];
                }
                double total = solar + hydro + diesel;
                double pct   = demand[d] > 0 ? total / demand[d] * 100 : 0;
                System.out.printf("%4d   %-6s %7.1f %7.1f %8.1f %7.1f %8.1f %6.1f%%%n",
                    hour, DISTRICTS[d], solar, hydro, diesel, total, demand[d], pct);

                for (int si = 0; si < avail.size(); si++)
                    totalCost += alloc[d][si] * avail.get(si).cost;
                totalRenew  += solar + hydro;
                if (diesel > 0) { totalDiesel += diesel; usedDiesel = true; }
            }
            if (usedDiesel && !dieselHours.contains(hour)) dieselHours.add(hour);
        }

        double kwh = totalRenew + totalDiesel;
        System.out.println("\n--- Cost & Resource Report ---");
        System.out.printf("Total cost       : Rs. %.2f%n", totalCost);
        System.out.printf("Total kWh        : %.1f%n", kwh);
        System.out.printf("Renewable %%      : %.1f%%%n", kwh > 0 ? totalRenew/kwh*100 : 0);
        System.out.printf("Diesel hours     : %s%n", dieselHours);
        System.out.println("Reason           : Solar unavailable after hour 18; demand exceeds Hydro capacity.");
        System.out.println("\nAlgorithm: Greedy (cheapest source first). O(H*S*D) time.");
        System.out.println("Trade-off: Fast and near-optimal. Full DP would be global optimal but O(H*2^S).");
    }
}