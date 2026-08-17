package com.oracle.trial.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns text into vector embeddings using OpenAI's embedding model, via
 * LangChain4j. Both document chunks (at upload time) and user questions
 * (at question time) go through this same service, so they live in the
 * same vector space and can be compared with similarity search.
 */
@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.embedding.model}") String modelName
    ) {
        OpenAiKeyValidator.validate(apiKey);
        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }

    /**
     * Embeds a piece of text and returns it as a List<Float>, which is the
     * shape Qdrant's REST API expects for the "vector" field.
     */
    public List<Float> embed(String text) {
        Embedding embedding = embeddingModel.embed(text).content();
        float[] vector = embedding.vector();

        List<Float> result = new ArrayList<>(vector.length);
        for (float value : vector) {
            result.add(value);
        }
        return result;
    }
}
