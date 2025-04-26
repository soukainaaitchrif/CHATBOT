package com.example.chatbottest.service;

import com.example.chatbottest.config.ModelLoader;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.*;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class EmbeddingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddingService.class);
    private static final int EMBEDDING_DIMENSION = 384;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public EmbeddingService(Dotenv dotenv) {
        try {
            LOGGER.info("Initializing OllamaEmbeddingModel...");
            this.embeddingModel = ModelLoader.getEmbeddingModel();
            LOGGER.info("EmbeddingModel initialized successfully.");
            this.embeddingStore = ModelLoader.getEmbeddingStore();
            LOGGER.info("EmbeddingStore initialized successfully.");
        } catch (Exception e) {
            LOGGER.error("Error during initialization: ", e);
            throw e;
        }
    }

    public EmbeddingStoreIngestor getIngestor(EmbeddingStore<TextSegment> embeddingStore) {
        DocumentSplitter documentsplitter = new DocumentByParagraphSplitter(1000, 100);
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(documentsplitter)
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
        return ingestor;
    }

    public Embedding embedText(String text) {
        return embeddingModel.embed(text).content();
    }

    public void addEmbedding(Embedding embedding, TextSegment textSegment) {
        embeddingStore.add(embedding, textSegment);
    }

    public List<TextSegment> findRelevantTextSegments(Embedding referenceEmbedding, int maxResults) {
        try {
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(referenceEmbedding)
                    .maxResults(maxResults)
                    .minScore(0.5)
                    .build();
            EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
            LOGGER.info("Search Results: {}", result.matches().size());
            return result.matches().stream()
                    .filter(match -> match.embedded() != null)
                    .map(EmbeddingMatch::embedded)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.error("Error during search: " + e.getMessage(), e);
            return List.of();
        }
    }
}