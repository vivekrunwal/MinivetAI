package com.vivek.core;

import ai.djl.Application;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;

/**
 * Simple DJL-based embedder for MiniLM model
 */
public class DjlMiniLmEmbedder implements AutoCloseable {
    private ZooModel<String, float[]> model;
    private Predictor<String, float[]> predictor;

    public DjlMiniLmEmbedder() throws Exception {
        // Load the all-MiniLM-L6-v2 model from HuggingFace
        Criteria<String, float[]> criteria = Criteria.builder()
                .setTypes(String.class, float[].class)
                .optModelUrls("djl://ai.djl.huggingface.pytorch/sentence-transformers/all-MiniLM-L6-v2")
                .optEngine("PyTorch")
                .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                .optApplication(Application.NLP.TEXT_EMBEDDING)
                .build();

        model = criteria.loadModel();
        predictor = model.newPredictor();
    }

    /**
     * Generate embeddings for the given text
     * 
     * @param text Input text to embed
     * @return Float array representing the text embedding
     * @throws TranslateException if embedding fails
     */
    public float[] embed(String text) throws TranslateException {
        return predictor.predict(text);
    }

    @Override
    public void close() {
        if (predictor != null) {
            predictor.close();
        }
        if (model != null) {
            model.close();
        }
    }
}
