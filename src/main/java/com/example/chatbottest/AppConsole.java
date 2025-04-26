package com.example.chatbottest;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.huggingface.HuggingFaceChatModel;
import dev.langchain4j.model.huggingface.HuggingFaceModelName;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.stream.Collectors;
//
public class AppConsole {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppConsole.class);
    private static final int EMBEDDING_DIMENSION = 384; // Dimension for Ollama embeddings
    private static final int MAX_RESULTS = 3; // Maximum number of relevant documents to retrieve
    private static final int CHAT_MEMORY_SIZE = 10; // Number of messages to retain in chat memory

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final MessageWindowChatMemory chatMemory;
    private final ChatLanguageModel model;

    public AppConsole() {
        Dotenv dotenv = Dotenv.load();
//        // Initialize Hugging Face model
//        this.HaginFaceModel = HuggingFaceChatModel.builder()
//                .accessToken(dotenv.get("HF_API_KEY"))
//                .modelId("sentence-transformers/all-MiniLM-L6-v2") // Ensure this model ID is correct
//                .timeout(Duration.ofSeconds(15))
//                .temperature(0.7)
//                .maxNewTokens(20)
//                .waitForModel(true)
//                .build();
//        this.model = OllamaChatModel.builder()
//                .baseUrl(dotenv.get("OLLAMA_BASE_URL"))
//                .modelName(dotenv.get("OLLAMA_MODEL_CHAT"))
//                .build();
        this.model = OpenAiChatModel.builder()
                .apiKey("demo")
                .modelName(OpenAiChatModelName.GPT_4_O_MINI)
                .timeout(Duration.ofSeconds(15))
                .temperature(0.7)
                .build();

        // Initialize Ollama embedding model
        this.embeddingModel = OllamaEmbeddingModel.builder()
                .baseUrl(dotenv.get("OLLAMA_BASE_URL"))
                .modelName(dotenv.get("OLLAMA_MODEL_NAME"))
                .build();

        // Initialize PgVector store
        this.embeddingStore = PgVectorEmbeddingStore.builder()
                .host(dotenv.get("DB_HOST"))
                .port(Integer.parseInt(dotenv.get("DB_PORT")))
                .database(dotenv.get("DB_NAME"))
                .user(dotenv.get("DB_USER"))
                .password(dotenv.get("DB_PASSWORD"))
                .table("rag")
                .dimension(EMBEDDING_DIMENSION)
                .createTable(true)
                .build();

        // Initialize Chat Memory
        this.chatMemory = MessageWindowChatMemory.withMaxMessages(CHAT_MEMORY_SIZE);
    }

    public void startConversation() {
        Scanner scanner = new Scanner(System.in);
        LOGGER.info("RAG Console Application Started (type 'exit' to quit)");
        LOGGER.info("----------------------------------------");
        chatMemory.clear();

        while (true) {
            System.out.print("\nYou: ");
            String userInput = scanner.nextLine().trim();

            if (userInput.equalsIgnoreCase("exit")) {
                LOGGER.info("Goodbye!");
                break;
            }

            try {
                // Add user message to chat memory
                chatMemory.add(new UserMessage(userInput));

                // Process user input with chat memory
                String response = processUserInput(userInput);
                System.err.println("\nAssistant: " + response);

                // Add assistant response to chat memory
                chatMemory.add(new AiMessage(response));
            } catch (Exception e) {
                LOGGER.error("Error processing user input: " + userInput, e);
                System.out.println("Error: " + e.getMessage());
            }
        }
        scanner.close();
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
            return new ArrayList<>();
        }
    }

    private String processUserInput(String userInput) {
        // Generate embedding for user query
        Embedding queryEmbedding = embeddingModel.embed(userInput).content();

        // Retrieve relevant documents
        List<TextSegment> relevantDocs = findRelevantTextSegments(queryEmbedding, MAX_RESULTS);

        // Build context from relevant documents
        StringBuilder context = new StringBuilder();
        for (TextSegment doc : relevantDocs) {
            context.append(doc.text()).append("\n");
        }

        // Build chat history
        StringBuilder chatHistory = new StringBuilder();
        for (ChatMessage message : chatMemory.messages()) {
            switch (message.type()) {
                case USER:
                    chatHistory.append("User: ").append(((UserMessage) message).singleText()).append("\n");
                    break;
                case AI:
                    chatHistory.append("Assistant: ").append(((AiMessage) message).text()).append("\n");
                    break;
                default:
                    LOGGER.warn("Unknown message type: {}", message.type());
            }
        }

        // Build the final prompt
        String prompt = String.format("""
                Context Information:
                %s
                
                Conversation History:
                %s
                
                User Question: %s
                
                Please provide a helpful answer based on the context information and conversation history.
                """, context.toString().trim(), chatHistory.toString().trim(), userInput);

        LOGGER.info("Generated Prompt:\n{}", prompt);

        // Generate response using the chat model
        return model.generate(prompt);
    }

    public void addDocument(String text) {
        try {
            Embedding embedding = embeddingModel.embed(text).content();
            Metadata metadata = new Metadata();
            metadata.put("source", "source_value");
            metadata.put("author", "author_name");
            metadata.put("documentId", UUID.randomUUID());
            embeddingStore.add(embedding, new TextSegment(text, metadata));
            LOGGER.info("Document added successfully: {}", text);
        } catch (Exception e) {
            LOGGER.error("Error adding document: {}", text, e);
            throw new RuntimeException("Failed to add document", e);
        }
    }

    public static void main(String[] args) {
        AppConsole app = new AppConsole();
        app.startConversation();
    }
}


//package org.main.presentation;
//
//import dev.langchain4j.agent.tool.ToolSpecification;
//import dev.langchain4j.data.document.Metadata;
//import dev.langchain4j.data.embedding.Embedding;
//import dev.langchain4j.data.message.AiMessage;
//import dev.langchain4j.data.message.ChatMessage;
//import dev.langchain4j.data.message.UserMessage;
//import dev.langchain4j.data.segment.TextSegment;
//import dev.langchain4j.memory.chat.MessageWindowChatMemory;
//import dev.langchain4j.model.chat.ChatLanguageModel;
//import dev.langchain4j.model.embedding.EmbeddingModel;
//import dev.langchain4j.model.huggingface.HuggingFaceChatModel;
//import dev.langchain4j.model.huggingface.HuggingFaceModelName;
//import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
//import dev.langchain4j.store.embedding.EmbeddingMatch;
//import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
//import dev.langchain4j.store.embedding.EmbeddingSearchResult;
//import dev.langchain4j.store.embedding.EmbeddingStore;
//import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
//import io.github.cdimascio.dotenv.Dotenv;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.time.Duration;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Scanner;
//import java.util.UUID;
//import java.util.stream.Collectors;
//
//public class AppConsole {
//
//    private static final Logger LOGGER = LoggerFactory.getLogger(AppConsole.class);
//    private static final int EMBEDDING_DIMENSION = 384; // Dimension for Ollama embeddings
//    private static final int MAX_RESULTS = 3; // Maximum number of relevant documents to retrieve
//    private static final int CHAT_MEMORY_SIZE = 10; // Number of messages to retain in chat memory
//
//    //    private final ChatLanguageModel chatModel;
//    private final EmbeddingModel embeddingModel;
//    private final EmbeddingStore<TextSegment> embeddingStore;
//    private final MessageWindowChatMemory chatMemory;
//    private final ChatLanguageModel HaginFaceModel;
//
//
//    public AppConsole() {
//        Dotenv dotenv = Dotenv.load();
//
//        // Initialize Ollama models
////        this.chatModel = OllamaChatModel.builder()
////                .baseUrl(dotenv.get("OLLAMA_BASE_URL"))
////                .modelName(dotenv.get("OLLAMA_MODEL_CHAT"))
////                .temperature(0.7)
////                .build();
//
//        this.HaginFaceModel = HuggingFaceChatModel.builder()
//                .accessToken(dotenv.get("HF_API_KEY"))
//                .modelId("GPT-OMNI/MINI-OMNI2")
//                .timeout(Duration.ofSeconds(15))
//                .temperature(0.7)
//                .maxNewTokens(20)
//                .waitForModel(true)
//                .build();
//
//        this.embeddingModel = OllamaEmbeddingModel.builder()
//                .baseUrl(dotenv.get("OLLAMA_BASE_URL"))
//                .modelName(dotenv.get("OLLAMA_MODEL_NAME"))
//                .build();
//
//        // Initialize PgVector store
//        this.embeddingStore = PgVectorEmbeddingStore.builder()
//                .host(dotenv.get("DB_HOST"))
//                .port(Integer.parseInt(dotenv.get("DB_PORT")))
//                .database(dotenv.get("DB_NAME"))
//                .user(dotenv.get("DB_USER"))
//                .password(dotenv.get("DB_PASSWORD"))
//                .table("rag")
//                .dimension(EMBEDDING_DIMENSION)
//                .createTable(true)
//                .build();
//
//        // Initialize Chat Memory
//        this.chatMemory = MessageWindowChatMemory.withMaxMessages(CHAT_MEMORY_SIZE);
//    }
//
//    public void startConversation() {
//        Scanner scanner = new Scanner(System.in);
//        LOGGER.info("RAG Console Application Started (type 'exit' to quit)");
//        LOGGER.info("----------------------------------------");
//        chatMemory.clear();
//        while (true) {
//            System.out.print("\nYou: ");
//            String userInput = scanner.nextLine().trim();
//
//            if (userInput.equalsIgnoreCase("exit")) {
//                LOGGER.info("Goodbye!");
//                break;
//            }
//
//            try {
//                // Add user message to chat memory
//                chatMemory.add(new UserMessage(userInput));
//
//                // Process user input with chat memory
//                String response = processUserInput(userInput);
//                System.err.println("\nAssistant: " + response);
//
//                // Add assistant response to chat memory
//                chatMemory.add(new AiMessage(response));
//            } catch (Exception e) {
//                LOGGER.error("Error processing user input: " + userInput, e);
//                System.out.println("Error: " + e.getMessage());
//            }
//        }
//        scanner.close();
//    }
//
//    public List<TextSegment> findRelevantTextSegments(Embedding referenceEmbedding, int maxResults) {
//        try {
//            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
//                    .queryEmbedding(referenceEmbedding)
//                    .maxResults(maxResults)
//                    .minScore(0.5)
//                    .build();
//
//            EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
//            LOGGER.info("Search Results: {}", result.matches().size());
//            return result.matches().stream()
//                    .filter(match -> match.embedded() != null)
//                    .map(EmbeddingMatch::embedded)
//                    .collect(Collectors.toList());
//        } catch (Exception e) {
//            LOGGER.error("Error during search: " + e.getMessage(), e);
//            return new ArrayList<>();
//        }
//    }
//
//    private String processUserInput(String userInput) {
//        // Generate embedding for user query
//        Embedding queryEmbedding = embeddingModel.embed(userInput).content();
//
//        // Retrieve relevant documents
//        List<TextSegment> relevantDocs = findRelevantTextSegments(queryEmbedding, MAX_RESULTS);
//
//        // Build context from relevant documents
//        StringBuilder context = new StringBuilder();
//        for (TextSegment doc : relevantDocs) {
//            context.append(doc.text()).append("\n");
//        }
//
//        // Build chat history
//        StringBuilder chatHistory = new StringBuilder();
//        for (ChatMessage message : chatMemory.messages()) {
//            switch (message.type()) {
//                case USER:
//                    chatHistory.append("User: ").append(((UserMessage) message).singleText()).append("\n");
//                    break;
//                case AI:
//                    chatHistory.append("Assistant: ").append(((AiMessage) message).text()).append("\n");
//                    break;
//                default:
//                    LOGGER.warn("Unknown message type: {}", message.type());
//            }
//        }
//
//        // Build the final prompt
//        String prompt = String.format("""
//                Context Information:
//                %s
//
//                Conversation History:
//                %s
//
//                User Question: %s
//
//                Please provide a helpful answer based on the context information and conversation history.
//                """, context.toString().trim(), chatHistory.toString().trim(), userInput);
//
//        LOGGER.info("Generated Prompt:\n{}", prompt);
//
//        // Generate response using the chat model
//        return HaginFaceModel.generate(prompt);
//    }
//
//    public void addDocument(String text) {
//        try {
//            Embedding embedding = embeddingModel.embed(text).content();
//            Metadata metadata = new Metadata();
//            metadata.put("source", "source_value");
//            metadata.put("author", "author_name");
//            metadata.put("documentId", UUID.randomUUID());
//            embeddingStore.add(embedding, new TextSegment(text, metadata));
//            LOGGER.info("Document added successfully: {}", text);
//        } catch (Exception e) {
//            LOGGER.error("Error adding document: {}", text, e);
//            throw new RuntimeException("Failed to add document", e);
//        }
//    }
//
//    public static void main(String[] args) {
//        AppConsole app = new AppConsole();
//        app.startConversation();
//    }
//}