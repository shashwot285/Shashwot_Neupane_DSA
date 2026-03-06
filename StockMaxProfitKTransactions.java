import java.util.*;

// Question 3: Agricultural Commodity Trading — Max Profit with K Transactions
// Find the maximum profit from at most k buy-sell transactions.
// Must sell before buying again. Cannot hold multiple lots simultaneously.
// Algorithm: DP with state compression. O(k*n) time, O(k) space.
public class StockMaxProfitKTransactions {

    static int maxProfit(int k, int[] prices) {
        int n = prices.length;
        if (n < 2 || k == 0) return 0;

        // If k >= n/2 we can capture every upward move (unlimited trades)
        if (k >= n / 2) {
            int profit = 0;
            for (int i = 1; i < n; i++)
                if (prices[i] > prices[i-1]) profit += prices[i] - prices[i-1];
            return profit;
        }

        // buy[j]  = best profit currently HOLDING stock, used at most j transactions
        // sell[j] = best profit NOT holding stock, used at most j transactions
        int[] buy  = new int[k + 1];
        int[] sell = new int[k + 1];
        Arrays.fill(buy, Integer.MIN_VALUE / 2);

        for (int price : prices) {
            for (int j = k; j >= 1; j--) {
                sell[j] = Math.max(sell[j], buy[j] + price);   // sell today
                buy[j]  = Math.max(buy[j],  sell[j-1] - price); // buy today
            }
        }
        return sell[k];
    }

    public static void main(String[] args) {
        // Example from assignment: buy@2000, sell@4000 = 2000 profit
        System.out.println("Example 1: " + maxProfit(2, new int[]{2000, 4000, 1000})); // 2000

        // Two windows: 3000->5000 (+2000) and 2000->6000 (+4000) = 6000
        System.out.println("Two windows: " + maxProfit(2, new int[]{3000, 5000, 2000, 6000})); // 6000

        // Declining: no profit possible
        System.out.println("Declining: " + maxProfit(2, new int[]{5000, 4000, 3000, 2000})); // 0

        // k=0: no trades
        System.out.println("k=0: " + maxProfit(0, new int[]{1000, 3000, 2000})); // 0

        // Classic: [3,2,6,5,0,3] k=2
        System.out.println("Classic k=2: " + maxProfit(2, new int[]{3, 2, 6, 5, 0, 3})); // 7
    }
}