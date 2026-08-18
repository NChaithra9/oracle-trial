package com.oracle.trial.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;

/**
 * Describes the visual content of a PDF page image (scanned text, charts,
 * diagrams, photos) using OpenAI's vision-capable chat model, via
 * LangChain4j.
 *
 * This is the simple way to make image-heavy pages searchable: instead of
 * adding a whole separate image-embedding pipeline (a different model, a
 * second Qdrant collection, new retrieval logic), we just turn the image
 * into a plain-text description once at upload time. That description is
 * then chunked, embedded, and stored exactly like any other page's text -
 * no other part of the RAG pipeline needs to know images were involved.
 */
@Service
public class ImageCaptionService {

    private final ChatLanguageModel visionModel;

    public ImageCaptionService(
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.chat.model}") String modelName
    ) {
        OpenAiKeyValidator.validate(apiKey);
        // Reuses the same chat model already configured for answering
        // questions (gpt-4o-mini by default) - it understands images too,
        // so this feature needs no extra model, dependency, or API key.
        this.visionModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.2)
                .build();
    }

    /**
     * @param pngImageBytes a single PDF page rendered as a PNG image
     * @return a plain-text description of everything useful on the page
     */
    public String describeImage(byte[] pngImageBytes) {
        String base64Image = Base64.getEncoder().encodeToString(pngImageBytes);

        UserMessage message = UserMessage.from(
                TextContent.from(
                        "This image is a page from a PDF document. Describe everything "
                                + "useful on it in plain text: transcribe any visible text, and "
                                + "describe the content of any charts, diagrams, tables, or "
                                + "photos. Be factual and specific, so this description alone "
                                + "can be used to answer questions about the page later."
                ),
                ImageContent.from(base64Image, "image/png")
        );

        List<ChatMessage> messages = List.of(message);
        Response<AiMessage> response = visionModel.generate(messages);
        return response.content().text().trim();
    }
}
