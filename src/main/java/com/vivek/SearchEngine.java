package com.vivek;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

public class SearchEngine {

    private final InvertedIndex invertedIndex;

    public SearchEngine(InvertedIndex invertedIndex) {
        this.invertedIndex = invertedIndex;
    }

    public Map<String, List<Integer>> searchWord(String term) {
        if (term == null || term.isBlank())
            return Collections.emptyMap();
        Map<String, Map<String, List<Integer>>> textBookSearchIndex = invertedIndex.buildInvertedIndexForTextSearch();
        return textBookSearchIndex.getOrDefault(term, Collections.emptyMap());
    }

    public Map<String, List<Integer>> searchPhrase(String phrase) {
        Map<String, Map<String, List<Integer>>> index = invertedIndex.buildInvertedIndexForTextSearch();

        if (phrase == null)
            return Collections.emptyMap();

        String[] tokens = phrase.split("\\s+");

        if (tokens.length == 0)
            return Collections.emptyMap();

        if (tokens.length == 1)
            return searchWord(tokens[0]);

        // Early check: if any token is missing entirely, nothing to do
        for (String t : tokens) {
            if (!index.containsKey(t)) return Collections.emptyMap();
        }

        Map<String, List<Integer>> firstMap = index.get(tokens[0]);
        Set<String> candidateBooks = new HashSet<>(firstMap.keySet());

        for (int i = 1; i < tokens.length; i++) {
            candidateBooks.retainAll(index.get(tokens[i]).keySet());
            if (candidateBooks.isEmpty())
                return Collections.emptyMap();
        }

        Map<String, List<Integer>> result = new HashMap<>();
        for (String book : candidateBooks) {
            List<Integer> firstPositions = firstMap.get(book);
            if (firstPositions == null || firstPositions.isEmpty())
                continue;

            List<Set<Integer>> subsequentPosSets = new ArrayList<>();
            for (int i = 1; i < tokens.length; i++) {
                List<Integer> positions = index.get(tokens[i]).get(book);
                subsequentPosSets.add(new HashSet<>(positions));
            }

            List<Integer> starts = new ArrayList<>();
            for (int p : firstPositions) {
                boolean consecutive = true;
                for (int i = 0; i < subsequentPosSets.size(); i++) {
                    if (!subsequentPosSets.get(i).contains(p + i + 1)) {
                        consecutive = false;
                        break;
                    }
                }
                if (consecutive) {
                    starts.add(p);
                }
            }

            if (!starts.isEmpty()) {
                result.put(book, starts);
            }
        }

        return result;
    }
}