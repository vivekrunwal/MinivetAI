package com.vivek;

/**
 * Main entry point for the simplified semantic search system
 */
public class Main {

    public static void main(String[] args) {

        // Normal Search by key
        SearchEngine searchEngine = new SearchEngine(new InvertedIndex());
        System.out.println(searchEngine.searchWord("murder"));

        System.out.println(searchEngine.searchPhrase("recorded in my notebook"));

    }

}