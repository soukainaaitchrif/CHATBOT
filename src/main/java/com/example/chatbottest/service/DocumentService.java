package com.example.chatbottest.service;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public class DocumentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentService.class);
    private final EmbeddingService embeddingService;

    public DocumentService(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    public void addDocument(String text) {
        try {
            Embedding embedding = embeddingService.embedText(text);
            // Check if the embedding already exists
            List<TextSegment> existingSegments = embeddingService.findRelevantTextSegments(embedding, 1);
            if (!existingSegments.isEmpty() && existingSegments.getFirst().text().equals(text)) {
                LOGGER.info("Duplicate document detected, skipping:");
                return;
            }
            Metadata metadata = new Metadata();
            metadata.put("source", "source_value");
            metadata.put("author", "author_name");
            metadata.put("documentId", UUID.randomUUID());
            embeddingService.addEmbedding(embedding, new TextSegment(text, metadata));
            LOGGER.info("Document added successfully: " );
        } catch (Exception e) {
            LOGGER.error("Error adding document: {}", text, e);
            throw new RuntimeException("Failed to add document", e);
        }
    }
}