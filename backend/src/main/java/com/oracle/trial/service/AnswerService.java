package com.oracle.trial.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Generates the final natural-language answer using OpenAI's chat model,
 * via LangChain4j. The model is instructed to answer strictly from the
 * supplied context (the chunks retrieved from Qdrant) and to say so
 * explicitly when the context doesn't contain the answer.
 */
@Service
public class AnswerService {

    public static final String NOT_FOUND_MESSAGE =
            "I couldn't find this information in the provided documents.";

    private final ChatLanguageModel chatModel;

    public AnswerService(
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.chat.model}") String modelName
    ) {
        OpenAiKeyValidator.validate(apiKey);
        this.chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.2)
                .build();
    }

    public String generateAnswer(String question, String context) {
        String prompt = """
                You are The Oracle, an assistant that answers questions using ONLY \
                the context extracted from the user's PDF documents below.

                Rules:
                1. Answer using only facts present in the context. Do not use outside knowledge.
                2. If the context does not contain enough information to answer the question, \
                reply with EXACTLY this sentence and nothing else: "%s"
                3. Keep the answer clear and concise.

                Context:
                %s

                Question:
                %s

                Answer:
                """.formatted(NOT_FOUND_MESSAGE, context, question);

        return chatModel.generate(prompt).trim();
    }
}
