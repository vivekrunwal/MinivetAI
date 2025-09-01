// MongoDB Shell Commands for Vector Search (run these in MongoDB Compass shell or mongosh)
// Note: These commands are for verification AFTER creating the vector index in Atlas UI

// 1. Connect to your database
use stories

// 2. Check if collection exists and count documents
db.lines.countDocuments()

// 3. View a sample document to verify structure
db.lines.findOne()

// 4. Check document structure with embedding
db.lines.findOne({}, {book: 1, lineNo: 1, text: 1, "embedding": {$slice: 5}})

// 5. Verify that embeddings exist and are arrays
db.lines.findOne({"embedding": {$exists: true}})

// 6. Check embedding dimensions (should be 384)
db.lines.aggregate([
  {$match: {"embedding": {$exists: true}}},
  {$project: {
    book: 1,
    embeddingLength: {$size: "$embedding"}
  }},
  {$limit: 1}
])

// 7. After vector index is created in Atlas UI, test vector search
// Replace the query vector with actual values from your embeddings
db.lines.aggregate([
  {
    $vectorSearch: {
      queryVector: [0.1, 0.2, 0.3], // Replace with actual 384-dimensional vector
      path: "embedding",
      numCandidates: 100,
      limit: 5
    }
  },
  {
    $project: {
      book: 1,
      lineNo: 1,
      text: 1,
      score: {$meta: "vectorSearchScore"}
    }
  }
])

// 8. Check collection statistics
db.lines.stats()

// 9. Find documents by book
db.lines.find({book: "A Study in Scarlet"}).limit(3)

// 10. Check index status (this won't show vector indexes, only regular indexes)
db.lines.getIndexes()
