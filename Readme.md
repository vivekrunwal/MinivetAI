# MinivetAI - Simple Semantic Search with DJL

A streamlined semantic search implementation using Deep Java Library (DJL) and MongoDB Vector Search.

## 🔍 Overview

This system converts text into high-dimensional vectors (embeddings) using the MiniLM model and uses MongoDB's vector search capabilities to find semantically similar content, even when exact words don't match.

## 🏗️ Architecture

```
Text Input → DJL MiniLM Model → Vector Embedding → MongoDB Storage
     ↓
Search Query → Query Embedding → Vector Search → Similarity Ranking → Results
```

## 📁 Key Components

### 1. **DjlMiniLmEmbedder** - Simple Embedding Engine
- Uses DJL to load sentence-transformers/all-MiniLM-L6-v2
- Generates 384-dimensional embeddings
- Handles model lifecycle automatically

### 2. **SimpleSemanticSearch** - Main Search Class
- Orchestrates embedding generation and MongoDB storage
- Handles vector search operations
- Manages connections and resources

### 3. **MongoDB Integration**
- Stores documents with their vector embeddings
- Performs vector similarity search using `$vectorSearch`
- Returns ranked results with similarity scores

## 🚀 Quick Start

### Prerequisites
1. **MongoDB Atlas** with vector search index configured
2. **Java 21+** 
3. **Maven**

### Setup

1. **Set MongoDB URI**:
   ```bash
   export MONGODB_URI="mongodb+srv://username:password@cluster.mongodb.net/"
   ```

2. **Create Vector Search Index** in MongoDB Atlas:
   - Database: `stories`
   - Collection: `lines`
   - Index Name: `vector_index`
   - Field: `embedding`
   - Dimensions: `384`
   - Similarity: `cosine`

3. **Run the Application**:
   ```bash
   mvn compile exec:java -Dexec.mainClass="com.vivek.driver.SimpleSemanticSearch"
   ```

## 📊 Example Results

### Query: "murder"
```
[0.893] The Adventure of the Bruce-Partington Plans:1212  "murder."
[0.881] The Valley of Fear:2462                          "murder."
[0.874] The Man with the Twisted Lip:1002               "murderer."
```

### Query: "detective solving mystery"
```
[0.879] A Study in Scarlet:694        "idea of a detective?"
[0.877] The Sign of Four:1886         "detective's name."
[0.855] The Hound of Baskervilles:2196 "detective?"
```

### Query: "criminal investigation" 
```
[0.940] The Adventure of Black Peter:654 "criminal investigation."
[0.913] The Adventure of Black Peter:442 "investigation."
[0.913] The Adventure of the Priory School:482 "investigation."
```

## 🎯 How It Works

### 1. **Document Processing**
```java
// For each text file in the dataset
for (String line : documentLines) {
    // Generate embedding using DJL
    float[] embedding = djlEmbedder.embed(line);
    
    // Store in MongoDB with metadata
    Document doc = new Document()
        .append("book", bookName)
        .append("lineNo", lineNumber)
        .append("text", line)
        .append("embedding", embeddingList);
    
    collection.insertOne(doc);
}
```

### 2. **Query Processing**
```java
// Convert search query to embedding
String query = "detective solving mystery";
float[] queryEmbedding = djlEmbedder.embed(query);

// Perform MongoDB vector search
List<Document> results = collection.aggregate([
    {
        "$vectorSearch": {
            "index": "vector_index",
            "queryVector": queryEmbedding,
            "path": "embedding",
            "numCandidates": 100,
            "limit": 10
        }
    }
]);
```

## 🔧 Features

### ⚡ **Smart Caching**
- Automatically skips re-embedding already processed books
- Reduces processing time from hours to seconds on subsequent runs
- Preserves existing embeddings

### 📚 **Complete Dataset Coverage**
- **4 Main Books**: A Study in Scarlet, Hound of Baskervilles, etc.
- **46+ Short Stories**: Adventures, Memoirs, Returns, Case-book, His Last Bow
- **Total**: 60+ complete stories with ~50,000+ embedded text lines

### 🎯 **Semantic Understanding**
- **Traditional Search**: "murder" only finds exact word "murder"
- **Semantic Search**: "murder" finds "assassination", "killing", "murderer", etc.
- **Context Aware**: Understands meaning beyond keywords

## 📋 Implementation Details

### **Model**: sentence-transformers/all-MiniLM-L6-v2
- **Dimensions**: 384
- **Similarity**: Cosine similarity
- **Framework**: Deep Java Library (DJL)
- **Performance**: ~384-dimensional embeddings per text line

### **MongoDB Vector Search**
   ```javascript
// Vector search pipeline
{
  "$vectorSearch": {
    "index": "vector_index",      // Index name in Atlas
    "queryVector": [0.1, 0.2, ...], // 384-dimensional query vector
    "path": "embedding",          // Field containing embeddings
    "numCandidates": 100,         // Candidates to consider
    "limit": 3                    // Results to return
  }
}
```

### **Performance Optimizations**
- **Batch Processing**: Inserts documents in batches of 100
- **Skip Logic**: Avoids re-processing existing books
- **Resource Management**: Proper cleanup of DJL resources
- **Connection Pooling**: Efficient MongoDB connections

## 🚀 Usage Examples

### Basic Search
   ```bash
mvn compile exec:java -Dexec.mainClass="com.vivek.driver.SimpleSemanticSearch"
   ```

### Interactive Menu (Traditional vs Semantic)
```bash
mvn compile exec:java -Dexec.mainClass="com.vivek.driver.Main"
```

## 🎓 Why This Works

### **Vector Similarity**
- Semantically similar text produces similar vector embeddings
- MongoDB finds documents with high cosine similarity
- Results ranked by semantic relevance, not keyword matching

### **Example Matches**
- Query: "assassin" → Finds: "murderer", "killer", "criminal"
- Query: "investigation" → Finds: "detective work", "solving case", "examination"
- Query: "mystery" → Finds: "puzzle", "enigma", "case", "problem"

## 🛠️ Dependencies

```xml
<!-- DJL Core -->
<dependency>
    <groupId>ai.djl</groupId>
    <artifactId>api</artifactId>
    <version>0.29.0</version>
</dependency>

<!-- DJL PyTorch Engine -->
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-engine</artifactId>
    <version>0.29.0</version>
</dependency>

<!-- HuggingFace Integration -->
<dependency>
    <groupId>ai.djl.huggingface</groupId>
    <artifactId>tokenizers</artifactId>
    <version>0.29.0</version>
</dependency>

<!-- MongoDB Driver -->
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>mongodb-driver-sync</artifactId>
    <version>4.11.0</version>
</dependency>
```

## 🔍 Troubleshooting

### **Common Issues**

1. **"index" is required**: Create vector search index in MongoDB Atlas
2. **Model download issues**: Ensure internet connectivity for DJL model download
3. **MongoDB authentication**: Verify MONGODB_URI is correctly set
4. **Memory issues**: Increase JVM heap size if processing large datasets

### **Vector Index Creation**
In MongoDB Atlas → Search Indexes → Create Search Index:
```json
{
  "name": "vector_index",
  "type": "vectorSearch", 
  "definition": {
    "fields": [{
      "type": "vector",
      "path": "embedding",
      "numDimensions": 384,
      "similarity": "cosine"
    }]
  }
}
```

## 📈 Performance Metrics

- **Initial Embedding**: ~71 minutes for complete dataset
- **Subsequent Runs**: ~26 seconds (with smart caching)
- **Search Latency**: Sub-second response times
- **Accuracy**: High semantic relevance scores (0.8-0.9+ for good matches)

## 🏆 Results Summary

✅ **Simple Setup**: No complex ONNX/BERT configuration  
✅ **Fast Performance**: 170x faster on subsequent runs  
✅ **High Accuracy**: Excellent semantic matching results  
✅ **Production Ready**: Robust error handling and resource management  
✅ **Scalable**: Efficient MongoDB vector storage and search  

---

*Built with ❤️ using DJL and MongoDB Vector Search*