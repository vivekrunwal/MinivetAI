package com.vivek;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SimpleSemanticSearch {
    private DjlMiniLmEmbedder embedder;
    private MongoClient mongoClient;
    private MongoCollection<Document> collection;

    public SimpleSemanticSearch(String mongoUri) throws Exception {
        this.embedder = new DjlMiniLmEmbedder();
        this.mongoClient = MongoClients.create(mongoUri);
        MongoDatabase database = mongoClient.getDatabase("stories");
        this.collection = database.getCollection("lines");
    }

    /**
     * Embed a single text file and store in MongoDB
     */
    public void embedFile(String filePath, String bookName) throws Exception {
        // Check if this book is already embedded
        long existingCount = collection.countDocuments(new Document("book", bookName));
        if (existingCount > 0) {
            System.out.println("Skipping " + bookName + " (already embedded with " + existingCount + " lines)");
            return;
        }
        
        System.out.println("Embedding " + bookName + "...");

        // Read file
        Path path = Paths.get(filePath);
        List<String> lines = Files.readAllLines(path);

        // Embed each line
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;

            try {
                float[] embedding = embedder.embed(line);
                List<Double> embeddingList = new ArrayList<>();
                for (float f : embedding) {
                    embeddingList.add((double) f);
                }

                Document doc = new Document()
                        .append("book", bookName)
                        .append("lineNo", i)
                        .append("text", line)
                        .append("embedding", embeddingList);

                documents.add(doc);

                if (documents.size() >= 100) { // Batch insert
                    collection.insertMany(documents);
                    documents.clear();
                    System.out.print(".");
                }
            } catch (Exception e) {
                System.err.println("Error embedding line " + i + ": " + e.getMessage());
            }
        }

        // Insert remaining documents
        if (!documents.isEmpty()) {
            collection.insertMany(documents);
        }

        System.out.println("\nEmbedded " + bookName + " successfully!");
    }

    /**
     * Perform semantic search
     */
    public List<Document> search(String query, int limit) throws Exception {
        // Embed the query
        float[] queryEmbedding = embedder.embed(query);
        List<Double> queryEmbeddingList = new ArrayList<>();
        for (float f : queryEmbedding) {
            queryEmbeddingList.add((double) f);
        }

        // MongoDB vector search pipeline
        List<Document> pipeline = Arrays.asList(
                new Document("$vectorSearch", new Document()
                        .append("index", "vector_index")
                        .append("queryVector", queryEmbeddingList)
                        .append("path", "embedding")
                        .append("numCandidates", 100)
                        .append("limit", limit)),
                new Document("$project", new Document()
                        .append("book", 1)
                        .append("lineNo", 1)
                        .append("text", 1)
                        .append("score", new Document("$meta", "vectorSearchScore")))
        );

        return collection.aggregate(pipeline).into(new ArrayList<>());
    }

    /**
     * Embed all books in the dataset
     */
    public void embedDataset() throws Exception {
        System.out.println("Starting dataset embedding...");
        Path datasetPath = Paths.get("src/main/resources/Dataset");

        if (!Files.exists(datasetPath)) {
            System.err.println("Dataset directory not found: " + datasetPath);
            return;
        }

        // Walk through entire dataset directory recursively (like buildInvertedIndexForTextSearch)
        try (var walk = Files.walk(datasetPath)) {
            walk.forEach(path -> {
                if (Files.isRegularFile(path) && path.toString().endsWith(".txt")) {
                    try {
                        String fileName = path.getFileName().toString().replace(".txt", "");
                        System.out.println("Processing: " + fileName + " (from " + path.getParent().getFileName() + ")");
                        embedFile(path.toString(), fileName);
                    } catch (Exception e) {
                        System.err.println("Error processing " + path + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public void close() {
        if (embedder != null) {
            embedder.close();
        }
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    public static void main(String[] args) throws Exception {
        String mongoUri = System.getenv("MONGODB_URI");
        SimpleSemanticSearch search = new SimpleSemanticSearch(mongoUri);
        try {
            System.out.println("Embedding dataset...");
            search.embedDataset();
            String[] queries = {"murder", "detective solving mystery", "criminal investigation"};

            for (String query : queries) {
                System.out.println("\nQuery: \"" + query + "\"");
                List<Document> results = search.search(query, 3);

                if (results.isEmpty()) {
                    System.out.println("No results found");
                } else {
                    for (int i = 0; i < results.size(); i++) {
                        Document doc = results.get(i);
                        double score = doc.getDouble("score");
                        String book = doc.getString("book");
                        int lineNo = doc.getInteger("lineNo");
                        String text = doc.getString("text");

                        System.out.printf("%d. [%.3f] %s:%d\n", i + 1, score, book, lineNo);
                        System.out.printf("   \"%s\"\n", text.length() > 100 ? text.substring(0, 100) + "..." : text);
                    }
                }
            }

        } finally {
            search.close();
        }
    }
}
