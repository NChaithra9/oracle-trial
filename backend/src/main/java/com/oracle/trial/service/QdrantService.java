package com.oracle.trial.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * All communication with Qdrant happens here, using Spring's RestClient to
 * call Qdrant's plain REST API directly (no Qdrant Java client library).
 *
 * Qdrant REST endpoints used:
 *  - GET  /collections/{name}                 -> check if the collection exists
 *  - PUT  /collections/{name}                 -> create the collection
 *  - PUT  /collections/{name}/points          -> upsert (store) vectors + metadata
 *  - POST /collections/{name}/points/query    -> similarity search
 */
@Service
public class QdrantService {

    private final RestClient restClient;
    private final String collectionName;
    private final int vectorSize;

    public QdrantService(
            @Value("${qdrant.url}") String qdrantUrl,
            @Value("${qdrant.collection}") String collectionName,
            @Value("${qdrant.vector.size}") int vectorSize
    ) {
        this.restClient = RestClient.builder().baseUrl(qdrantUrl).build();
        this.collectionName = collectionName;
        this.vectorSize = vectorSize;
    }

    /**
     * Runs once at startup so the collection is guaranteed to exist before
     * any upload or question is handled.
     */
    @PostConstruct
    public void ensureCollectionExists() {
        boolean exists;
        try {
            restClient.get()
                    .uri("/collections/{name}", collectionName)
                    .retrieve()
                    .toBodilessEntity();
            exists = true;
        } catch (RestClientResponseException e) {
            exists = false;
        }

        if (!exists) {
            createCollection();
        }
    }

    private void createCollection() {
        Map<String, Object> vectorsConfig = Map.of(
                "size", vectorSize,
                "distance", "Cosine"
        );
        Map<String, Object> body = Map.of("vectors", vectorsConfig);

        restClient.put()
                .uri("/collections/{name}", collectionName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Stores one chunk's embedding vector together with its metadata
     * (document name, page number, chunk index, and the original text).
     */
    public void upsertPoint(String pointId, List<Float> vector, Map<String, Object> payload) {
        Map<String, Object> point = new HashMap<>();
        point.put("id", pointId);
        point.put("vector", vector);
        point.put("payload", payload);

        Map<String, Object> body = Map.of("points", List.of(point));

        restClient.put()
                .uri("/collections/{name}/points?wait=true", collectionName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Finds the most similar stored chunks to the given vector, using
     * Qdrant's current "/points/query" endpoint (the older "/points/search"
     * endpoint works the same way but is deprecated).
     * Each result map contains "score" and "payload" (the metadata we stored).
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> search(List<Float> vector, int topK) {
        Map<String, Object> body = Map.of(
                "query", vector,
                "limit", topK,
                "with_payload", true
        );

        Map<String, Object> response = restClient.post()
                .uri("/collections/{name}/points/query", collectionName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("result") == null) {
            return List.of();
        }

        Map<String, Object> result = (Map<String, Object>) response.get("result");
        Object points = result.get("points");
        if (points == null) {
            return List.of();
        }
        return (List<Map<String, Object>>) points;
    }
}
