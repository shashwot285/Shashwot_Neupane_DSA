import java.util.*;

// Question 1b: Word Break — All Sentences
// Given a continuous user search string and a marketing keyword dictionary,
// return all possible sentences where every word is a valid keyword.
// Algorithm: Backtracking + memoization (top-down DP). O(n^2 * 2^n) worst case.
public class WordBreakAllSentences {

    static Map<Integer, List<String>> memo;
    static Set<String> dict;
    static String query;

    static List<String> wordBreak(String userQuery, List<String> dictionary) {
        query = userQuery;
        dict  = new HashSet<>(dictionary);
        memo  = new HashMap<>();
        return backtrack(0);
    }

    static List<String> backtrack(int start) {
        if (memo.containsKey(start)) return memo.get(start);

        List<String> results = new ArrayList<>();
        if (start == query.length()) { results.add(""); return results; }

        for (int end = start + 1; end <= query.length(); end++) {
            String word = query.substring(start, end);
            if (dict.contains(word)) {
                for (String rest : backtrack(end))
                    results.add(rest.isEmpty() ? word : word + " " + rest);
            }
        }
        memo.put(start, results);
        return results;
    }

    public static void main(String[] args) {
        // Example 1
        System.out.println("Example 1: " +
            wordBreak("nepaltrekkingguide",
                Arrays.asList("nepal","trekking","guide","nepaltrekking")));
        // Expected: [nepal trekking guide, nepaltrekking guide]

        // Example 2
        System.out.println("Example 2: " +
            wordBreak("visitkathmandunepal",
                Arrays.asList("visit","kathmandu","nepal","visitkathmandu","kathmandunepal")));
        // Expected: [visit kathmandu nepal, visitkathmandu nepal, visit kathmandunepal]

        // Example 3: no valid segmentation
        System.out.println("Example 3: " +
            wordBreak("everesthikingtrail",
                Arrays.asList("everest","hiking","trek")));
        // Expected: []
    }
}