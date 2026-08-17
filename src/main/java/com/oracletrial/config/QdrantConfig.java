package com.oracletrial.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

/**
 * Connects the application to Qdrant, the vector database that stores
 * document chunk embeddings.
 *
 * <p>Qdrant does not create collections automatically, so on startup this
 * checks whether the configured collection exists and creates it (using
 * cosine similarity and a vector size that matches the embedding model) if
 * it does not. That way a beginner can point the app at a brand-new Qdrant
 * instance and it just works, with no manual setup step.</p>
 */
@Slf4j
@Configuration
public class QdrantConfig {

    @Value("${qdrant.url}")
    private String qdrantUrl;

    @Value("${qdrant.api-key:}")
    private String apiKey;

    @Value("${qdrant.collection}")
    private String collectionName;

    /**
     * Creates the LangChain4j {@link EmbeddingStore} backed by Qdrant.
     *
     * @param embeddingModel used once at startup to work out the vector size
     *                        the collection needs
     * @return a Qdrant-backed embedding store for {@link TextSegment}s
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(EmbeddingModel embeddingModel) {
        URI uri = URI.create(qdrantUrl);
        String host = uri.getHost() != null ? uri.getHost() : "localhost";
        int port = uri.getPort() != -1 ? uri.getPort() : 6334;
        boolean useTls = "https".equalsIgnoreCase(uri.getScheme());

        ensureCollectionExists(host, port, useTls, embeddingModel.dimension());

        QdrantEmbeddingStore.Builder builder = QdrantEmbeddingStore.builder()
                .host(host)
                .port(port)
                .useTls(useTls)
                .collectionName(collectionName);

        if (apiKey != null && !apiKey.isBlank()) {
            builder.apiKey(apiKey);
        }

        return builder.build();
    }

    /**
     * Creates the Qdrant collection with cosine similarity if it is missing.
     *
     * <p>Failures here (e.g. Qdrant not running yet) are logged as a
     * warning rather than thrown, so the rest of the application can still
     * start up. Upload/search/ask requests will simply fail with a clear
     * error until Qdrant is reachable.</p>
     */
    private void ensureCollectionExists(String host, int port, boolean useTls, int vectorSize) {
        QdrantGrpcClient.Builder grpcBuilder = QdrantGrpcClient.newBuilder(host, port, useTls);
        if (apiKey != null && !apiKey.isBlank()) {
            grpcBuilder.withApiKey(apiKey);
        }

        try (QdrantClient client = new QdrantClient(grpcBuilder.build())) {
            boolean exists = client.collectionExistsAsync(collectionName).get();
            if (exists) {
                log.info("Qdrant collection '{}' already exists", collectionName);
                return;
            }

            client.createCollectionAsync(
                    collectionName,
                    VectorParams.newBuilder()
                            .setDistance(Distance.Cosine)
                            .setSize(vectorSize)
                            .build()
            ).get();
            log.info("Created Qdrant collection '{}' (size={}, distance=cosine)", collectionName, vectorSize);
        } catch (Exception e) {
            log.warn("Could not verify/create Qdrant collection '{}' at {}:{} - is Qdrant running and "
                    + "is QDRANT_URL correct? Document upload and search will fail until this is fixed.",
                    collectionName, host, port, e);
        }
    }
}
