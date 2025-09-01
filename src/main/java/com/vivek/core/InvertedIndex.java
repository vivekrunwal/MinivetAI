package com.vivek.core;

import com.vivek.constants.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class InvertedIndex {

    // contains:
    public Map<String, List<Integer>> tokensMyBookData(List<String> lines) {
        int index = 0;
        Map<String, List<Integer>> wordToIndices = new HashMap<>();
        for (String line : lines) {
            String[] words = line.split("\\s+");
            // gets rid of all spaces and new tabes / new lines
            for (String word : words) {
                if (word.isBlank()) {
                    continue;
                }
                String wordWithoutSpecialCharacter = word.replaceAll(Constants.SPECIAL_CHARACTER_REGEX, "");
                wordToIndices.computeIfAbsent(wordWithoutSpecialCharacter.toLowerCase(), k -> new ArrayList<>()).add(index++);
            }
        }
        return wordToIndices;
    }



    public Map<String, Map<String, List<Integer>>> buildInvertedIndexForTextSearch(){
         Path dirPath = Paths.get("src/main/resources/Dataset");
         Map<String, Map<String, List<Integer>>> textBookSearchMap = new HashMap<>();

         try (Stream<Path> walk = Files.walk(dirPath)) {
             walk.forEach(path -> {
                 if (Files.isRegularFile(path)) {
                     System.out.println("File: " + path.getFileName());
                     System.out.println(path);
                     String fileName = path.toString();
                     if (fileName.endsWith(".txt")) {
                         // To read the file content, you would use Files.readAllLines, etc.
                         // Example: readContent(path);
                         // filter .txt
                         try {
                             List<String> lines = Files.readAllLines(path.toAbsolutePath());
                             Map<String, List<Integer>> wordsToIndicesMap = tokensMyBookData(lines); // not required
                             String bookName = path.getFileName().toString().replaceAll(".txt","");
                             for (Map.Entry<String, List<Integer>> e : wordsToIndicesMap.entrySet()) {
                                 String term = e.getKey();
                                 List<Integer> positions = e.getValue();
                                 textBookSearchMap
                                         .computeIfAbsent(term, k -> new HashMap<>())
                                         .put(bookName, new ArrayList<>(positions));
                             }

                             // for each line: convert it to embeded vctor
                             // list of vestcor

                             // now, if you eant to seacrh assassin: // embed vector
                             // pg vector: order by similarity

                         } catch (IOException e) {
                             throw new RuntimeException(e);
                         }
                     }
                 } else if (Files.isDirectory(path) && !path.equals(dirPath)) {
                     System.out.println("Folder: " + path.getFileName());
                 }
             });
         } catch (IOException e) {
             e.printStackTrace();
         }
         return textBookSearchMap;
     }
}
