// EmbeddFiles.java
package com.example.chatbottest;

import com.example.chatbottest.service.DocumentService;
import com.example.chatbottest.service.EmbeddingService;
import io.github.cdimascio.dotenv.Dotenv;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class EmbeddFiles {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddFiles.class);

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();
        EmbeddingService embeddingService = new EmbeddingService(dotenv);
        DocumentService documentService = new DocumentService(embeddingService);
        File pdfFolder = new File("src/main/resources/PDF");
        File[] pdfFiles = pdfFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));

        if (pdfFiles != null) {
            for (File pdfFile : pdfFiles) {
                try (PDDocument document = PDDocument.load(pdfFile)) {
                    PDFTextStripper pdfStripper = new PDFTextStripper();
                    String documentText = pdfStripper.getText(document);
                    documentService.addDocument(documentText);
//                    LOGGER.info("Document {} added successfully.", pdfFile.getName());
                } catch (IOException e) {
                    LOGGER.error("Failed to process document {}: {}", pdfFile.getName(), e.getMessage());
                }
            }
        } else {
            LOGGER.warn("No PDF files found in the specified folder.");
        }
    }
}