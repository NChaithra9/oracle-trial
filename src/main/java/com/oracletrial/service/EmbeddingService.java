package com.oracletrial.service;

import com.oracletrial.model.DocumentChunk;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Generates embeddings for text and stores them in Qdrant.
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
    private final EmbeddingStore<TextSegment> embeddingStore;

    public EmbeddingService(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    /**
     * Converts a piece of text into an embedding vector.
     *
     * @param text text to embed (a document chunk, or a user's question)
     * @return the embedding as an array of numbers
     */
    public float[] embedText(String text) {
        Embedding embedding = embeddingModel.embed(text).content();
        float[] vector = embedding.vector();

        log.debug("Generated embedding of size {}", vector.length);

        return vector;
    }

    /**
     * Embeds a document chunk and stores the vector plus its citation
     * metadata (document name, chunk number, page number) in Qdrant, so it
     * can later be found by {@code RagService}.
     *
     * @param chunk chunk to embed and store
     */
    public void save(DocumentChunk chunk) {
        Embedding embedding = embeddingModel.embed(chunk.getText()).content();

        Metadata metadata = new Metadata()
                .put("document", chunk.getDocument())
                .put("chunk", chunk.getChunk());
        if (chunk.getPage() != null) {
            metadata.put("page", chunk.getPage());
        }

        TextSegment segment = TextSegment.from(chunk.getText(), metadata);
        embeddingStore.add(embedding, segment);

        log.debug("Stored chunk {} of {} in Qdrant", chunk.getChunk(), chunk.getDocument());
    }
}
