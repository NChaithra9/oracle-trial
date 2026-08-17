package com.oracle.trial.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Splits a page of text into smaller overlapping chunks before it is
 * embedded. Chunking keeps each embedded piece small and focused, which
 * makes similarity search more accurate, while the overlap prevents a
 * sentence that spans a chunk boundary from losing its meaning.
 *
 * Sizes are expressed in characters, but we split on word boundaries so
 * words are never cut in half.
 */
@Service
public class ChunkingService {

    private static final int AVG_CHARS_PER_WORD = 6;

    public List<String> chunkText(String text, int chunkSizeChars, int overlapChars) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        String[] words = text.trim().split("\\s+");
        int wordsPerChunk = Math.max(20, chunkSizeChars / AVG_CHARS_PER_WORD);
        int overlapWords = Math.max(0, overlapChars / AVG_CHARS_PER_WORD);

        int start = 0;
        while (start < words.length) {
            int end = Math.min(start + wordsPerChunk, words.length);
            String chunk = String.join(" ", Arrays.asList(words).subList(start, end));
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }

            if (end >= words.length) {
                break;
            }

            // Step forward, but re-include the last `overlapWords` words
            // in the next chunk for context continuity.
            int nextStart = end - overlapWords;
            start = Math.max(nextStart, start + 1);
        }

        return chunks;
    }
}
