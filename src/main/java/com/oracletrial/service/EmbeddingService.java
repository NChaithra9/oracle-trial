package com.oracletrial.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Generates embeddings for text using LangChain4j.
 *
 * <p>An embedding is a list of numbers that represents the meaning of a
 * piece of text. Two chunks with similar meaning end up with similar
 * numbers, which is exactly what makes similarity search possible once
 * chunks are stored in the vector database.</p>
 */
@Slf4j
@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * Converts a piece of text into an embedding vector.
     *
     * @param text chunk text to embed
     * @return the embedding as an array of numbers
     */
    public float[] embedText(String text) {
        Embedding embedding = embeddingModel.embed(text).content();
        float[] vector = embedding.vector();

        log.debug("Generated embedding of size {}", vector.length);

        return vector;
    }
}
