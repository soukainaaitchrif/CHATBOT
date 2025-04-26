package com.example.chatbottest.service;

import com.example.chatbottest.config.ModelLoader;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

public class ChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatService.class);
    //TODO: Change this to the actual number of results you want to retrieve
    private static final int MAX_RESULTS = 3;
    //TODO: Change this to the actual chat memory size
    private static final int CHAT_MEMORY_SIZE = 10;
    private final ChatLanguageModel model;
    private final MessageWindowChatMemory chatMemory;
    private final EmbeddingService embeddingService;

    public ChatService(Dotenv dotenv, EmbeddingService embeddingService) {
        this.model = ModelLoader.getChatModel();
        this.embeddingService = embeddingService;
        this.chatMemory = MessageWindowChatMemory.withMaxMessages(CHAT_MEMORY_SIZE);
    }

    public void clearChatMemory() {
        chatMemory.clear();
    }

    public String processUserInput(String userInput) {
        chatMemory.add(new UserMessage(userInput));
        String response = generateResponse(userInput);
        chatMemory.add(new AiMessage(response));
        return response;
    }

    private String generateResponse(String userInput) {
        var queryEmbedding = embeddingService.embedText(userInput);
        var relevantDocs = embeddingService.findRelevantTextSegments(queryEmbedding, MAX_RESULTS);

        String context = relevantDocs.stream()
                .map(doc -> doc.text())
                .collect(Collectors.joining("\n"));

        String chatHistory = chatMemory.messages().stream()
                .map(this::formatMessage)
                .collect(Collectors.joining("\n"));

        String prompt = String.format("""
                Context Information:
                %s
                
                Conversation History:
                %s
                
                User Question: %s
                
                Please provide a helpful answer based on the context information and conversation history.
                """, context.trim(), chatHistory.trim(), userInput);

        LOGGER.info("Generated Prompt:\n{}", prompt);
        return model.generate(prompt);
    }

    private String formatMessage(ChatMessage message) {
        switch (message.type()) {
            case USER:
                return "User: " + ((UserMessage) message).singleText();
            case AI:
                return "Assistant: " + ((AiMessage) message).text();
            default:
                LOGGER.warn("Unknown message type: {}", message.type());
                return "";
        }
    }
}