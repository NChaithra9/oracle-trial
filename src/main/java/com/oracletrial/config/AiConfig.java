package com.oracletrial.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the AI model clients used by the RAG pipeline.
 *
 * <p>The rest of the application only depends on LangChain4j's
 * {@link EmbeddingModel} interface, not on how it is set up. This keeps the
 * provider details (API key, model name) in one small, easy-to-find place.</p>
 */
@Configuration
public class AiConfig {

    @Value("${openai.api-key}")
    private String openAiApiKey;

    /**
     * Creates the embedding model used to turn text into vectors.
     *
     * @return a LangChain4j embedding model backed by an OpenAI-compatible API
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(openAiApiKey)
                .modelName("text-embedding-3-small")
                .build();
    }
}
