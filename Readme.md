# Complete Semantic Search Flow with MongoDB Vector Search

This guide explains the complete implementation of semantic search using BERT embeddings and MongoDB vector search capabilities.

## 🔍 Overview

The semantic search system converts text into high-dimensional vectors (embeddings) and uses MongoDB's vector search capabilities to find semantically similar content, even when exact words don't match.

## 🏗️ Architecture

```
Text Input → BERT Tokenization → ONNX Model → Vector Embedding → MongoDB Storage
     ↓
Search Query → Query Embedding → Vector Search → Similarity Ranking → Results
```

## 📁 Key Components

### 1. **BertMongoEmbedder** - Main Integration Class
- Orchestrates BERT embedding generation and MongoDB storage
- Handles vector search operations
- Manages resources and connections

### 2. **SimpleBertEmbedder** - BERT Model Interface
- Loads ONNX BERT models
- Tokenizes text using custom tokenizer
- Generates 384-dimensional embeddings

### 3. **WordPieceTokenizer** - Text Preprocessing
- Implements WordPiece tokenization
- Handles special tokens ([CLS], [SEP], [PAD], [UNK])
- Converts text to model-compatible input

### 4. **MongoDB Integration**
- Stores documents with their vector embeddings
- Performs vector similarity search using `$vectorSearch`
- Returns ranked results with similarity scores

## 🚀 Complete Flow Demonstration

### Step 1: Document Processing Pipeline

```java
// For each text document:
String[] lines = readTextFile(bookPath);

// For each line of text:
for (String line : lines) {
    // 1. Convert text to embedding vector
    float[] embedding = bertEmbedder.embed(line);
    
    // 2. Store in MongoDB with metadata
    Document doc = new Document()
        .append("book", bookName)
        .append("lineNo", lineNumber)
        .append("text", line)
        .append("embedding", embedding);
    
    mongoCollection.insertOne(doc);
}
```

### Step 2: Query Processing Pipeline

```java
// When user searches for "assassin":
String query = "assassin";

// 1. Convert query to embedding vector
float[] queryEmbedding = bertEmbedder.embed(query);

// 2. Perform MongoDB vector search
List<Document> results = mongoCollection.aggregate([
    {
        "$vectorSearch": {
            "queryVector": queryEmbedding,
            "path": "embedding",
            "numCandidates": 100,
            "limit": 10
        }
    },
    {
        "$project": {
            "book": 1,
            "lineNo": 1,
            "text": 1,
            "score": { "$meta": "vectorSearchScore" }
        }
    }
]);
```

## 🎯 Why This Works

### Semantic Understanding
- **Traditional Search**: Finds exact word matches ("murder" ≠ "assassination")
- **Semantic Search**: Understands meaning ("murder" ≈ "assassination" ≈ "killing")

### Vector Similarity
- Text with similar meanings produce similar vector embeddings
- MongoDB vector search finds documents with high cosine similarity
- Results are ranked by semantic relevance, not just keyword matching

## 📊 Example Results

### Query: "assassin"
```
[0.8234] The Hound of Baskervilles:156  "The man who killed Sir Charles was no ordinary murderer..."
[0.7891] A Study in Scarlet:89         "This death was clearly the work of a professional killer..."
[0.7654] The Valley of Fear:234        "The criminal organization had hired a deadly operative..."
```

### Query: "detective solving mystery"
```
[0.9012] The Adventures:45    "Holmes examined the evidence with his characteristic precision..."
[0.8567] The Memoirs:123     "Watson watched as his friend unraveled the complex case..."
[0.8234] The Return:67       "The brilliant investigator pieced together the clues..."
```

## 🛠️ Setup and Usage

### Prerequisites
1. **BERT Model Files**: Download to `models/` directory
   - `all-MiniLM-L6-v2.onnx`
   - `all-MiniLM-L6-v2-tokenizer.json`

2. **MongoDB Atlas**: Set up vector search index
   ```javascript
   {
     "type": "vector",
     "path": "embedding",
     "numDimensions": 384,
     "similarity": "cosine"
   }
   ```

3. **Environment Variables**:
   ```bash
   export MONGODB_URI="mongodb+srv://username:password@cluster.mongodb.net/"
   ```

### Running the Demo

```bash
# Run comprehensive semantic search demonstration
mvn compile exec:java -Dexec.mainClass="com.vivek.Main" -Dexec.args="demo"

# Run traditional search comparison
mvn compile exec:java -Dexec.mainClass="com.vivek.Main"
```

## 📋 Flow Steps in Detail

### 1. **Initialization**
- Load BERT ONNX model and tokenizer
- Connect to MongoDB Atlas
- Verify vector search index exists

### 2. **Document Embedding**
```java
// Process each document
for (String line : documentLines) {
    // Tokenize: "Hello world" → ["[CLS]", "hello", "world", "[SEP]", "[PAD]"...]
    int[] tokens = tokenizer.tokenize(line);
    
    // Generate embedding: tokens → 384-dimensional vector
    float[] embedding = bertModel.forward(tokens);
    
    // Store with metadata
    storeInMongoDB(line, embedding, metadata);
}
```

### 3. **Query Processing**
```java
// Convert query to same vector space
String query = "murder investigation";
float[] queryVector = bertModel.embed(query);

// Find similar vectors in MongoDB
List<Document> results = performVectorSearch(queryVector);

```

Here’s what that aggregation pipeline means, line by line, in simple words.

# Big picture

You’re running a **nearest-neighbors (semantic) search** in MongoDB using the prebuilt **vector index** on the field `embedding`.
Stage 1 finds the documents whose `embedding` vectors are closest to your **query vector**;
Stage 2 chooses which fields to return and includes a **relevance score**.

---

## Stage 1 — `$vectorSearch`

```java
new Document("$vectorSearch", new Document()
  .append("index", "vector_index")
  .append("queryVector", queryEmbeddingList)
  .append("path", "embedding")
  .append("numCandidates", 100)
  .append("limit", limit))
```

* **`index: "vector_index"`**
  The name of your **Atlas Vector Search** index. You must have created this index (on `embedding`) with the right vector **dimension** and similarity metric (e.g., cosine).

* **`queryVector: queryEmbeddingList`**
  The numeric array for the **query embedding** (same dimension as the stored `embedding` field). This is what MongoDB compares against each document’s vector.

* **`path: "embedding"`**
  The document field that holds your vectors. In your collection each doc has `"embedding": [ ... numbers ... ]`.

* **`numCandidates: 100`**
  How many **nearest neighbors** to consider from the index before final scoring.
  Think of it as: “pull the top \~100 rough matches from the ANN index, then compute exact similarity and pick the best ones.”
  Higher = potentially better accuracy but more work.

* **`limit: limit`**
  How many **final results** to return to your app (e.g., top 3).

> TL;DR: This stage says “from the vector index, find the \~100 closest vectors to my query, then return the top N (limit) best matches.”

---

## Stage 2 — `$project`

```java
new Document("$project", new Document()
  .append("book", 1)
  .append("lineNo", 1)
  .append("text", 1)
  .append("score", new Document("$meta", "vectorSearchScore")))
```

* **`book`, `lineNo`, `text`: 1**
  Include these original fields in the output.

* **`score: { $meta: "vectorSearchScore" }`**
  Attach the **similarity score** computed by `$vectorSearch`.
  For cosine similarity, **higher score means more similar**. You print this to rank/inspect results.

---

## Helpful tips

* The index `"vector_index"` must be created on `embedding` with the **exact same dimension** as your model output (e.g., 384 for MiniLM-L6-v2).
* You can filter within `$vectorSearch` (optional) to narrow search, e.g. only one book:

  ```java
  .append("filter", new Document("book", "SherlockHolmes"))
  ```
* Tuning:

   * Increase `numCandidates` if you need better recall (slower).
   * Keep `limit` small for faster responses.
* If your metric is cosine similarity, it’s common to store **L2-normalized** vectors (unit length). Make sure your index config and stored vectors align with the metric you chose.

That’s it: Stage 1 finds the closest vectors; Stage 2 returns the fields you care about plus the relevance `score`.


### 4. **Similarity Ranking**
- MongoDB calculates cosine similarity between query vector and stored embeddings
- Returns top matches with similarity scores (0.0 to 1.0)
- Higher scores indicate greater semantic similarity

## 🔧 Technical Details

### Vector Dimensions
- **Model**: all-MiniLM-L6-v2 (384 dimensions)
- **Similarity**: Cosine similarity
- **Index Type**: MongoDB Atlas Vector Search

### Performance Considerations
- **Batch Processing**: Process documents in chunks for efficiency
- **Memory Management**: Close ONNX resources properly
- **Connection Pooling**: Reuse MongoDB connections

### Error Handling
- Model loading failures
- MongoDB connection issues
- Vector search index missing
- Memory exhaustion during large document processing

## 🎓 Learning Outcomes

This implementation demonstrates:
1. **End-to-end ML Pipeline**: From raw text to searchable vectors
2. **Production Integration**: Using MongoDB for scalable vector storage
3. **Semantic Understanding**: Going beyond keyword matching
4. **Real-world Application**: Practical search improvement techniques

## 🚀 Next Steps

1. **Batch Processing**: Implement chunk-based document processing
2. **Index Optimization**: Fine-tune MongoDB vector search parameters
3. **Hybrid Search**: Combine semantic and traditional search results
4. **Performance Monitoring**: Track search latency and accuracy metrics

