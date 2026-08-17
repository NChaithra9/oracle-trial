package com.oracletrial.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the AI model clients used by the RAG pipeline.
 *
 * <p>The rest of the application only depends on LangChain4j's
 * {@link EmbeddingModel} and {@link ChatModel} interfaces, not on how they
 * are set up. This keeps provider details (API key, model names) in one
 * small, easy-to-find place, and makes it possible to swap providers later
 * by changing only this class.</p>
 */
@Configuration
public class AiConfig {

    @Value("${openai.api-key}")
    private String openAiApiKey;

    @Value("${openai.embedding-model}")
    private String embeddingModelName;

    @Value("${openai.chat-model}")
    private String chatModelName;

    /**
     * Creates the embedding model used to turn text into vectors.
     *
     * @return a LangChain4j embedding model backed by an OpenAI-compatible API
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(openAiApiKey)
                .modelName(embeddingModelName)
                .build();
    }

    /**
     * Creates the chat model used to turn retrieved chunks into a natural
     * language answer.
     *
     * @return a LangChain4j chat model backed by an OpenAI-compatible API
     */
    @Bean
    public ChatModel chatModel() {
        return OpenAiChatModel.builder()
                .apiKey(openAiApiKey)
                .modelName(chatModelName)
                // Temperature 0 makes the model stick to the given context
                // instead of creatively filling in plausible-sounding
                // details that were never actually in the documents.
                .temperature(0.0)
                .build();
    }
}
