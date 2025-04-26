package com.example.chatbottest;

import io.github.cdimascio.dotenv.Dotenv;
import com.example.chatbottest.service.ChatService;
import com.example.chatbottest.service.DocumentService;
import com.example.chatbottest.service.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

public class ConsoleTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleTest.class);
    private final ChatService chatService;
    private final DocumentService documentService;

    public ConsoleTest() {
        Dotenv dotenv = Dotenv.load();
        EmbeddingService embeddingService = new EmbeddingService(dotenv);
        this.chatService = new ChatService(dotenv, embeddingService);
        this.documentService = new DocumentService(embeddingService);
    }

    public void startConversation() {
        Scanner scanner = new Scanner(System.in);
        LOGGER.info("RAG Console Application Started (type 'exit' to quit)");
        LOGGER.info("----------------------------------------");
        chatService.clearChatMemory();

        while (true) {
            System.out.print("\nYou: ");
            String userInput = scanner.nextLine().trim();

            if (userInput.equalsIgnoreCase("exit")) {
                LOGGER.info("Goodbye!");
                break;
            }

            try {
                String response = chatService.processUserInput(userInput);
                System.err.println("\nAssistant: " + response);
            } catch (Exception e) {
                LOGGER.error("Error processing user input: " + userInput, e);
                System.out.println("Error: " + e.getMessage());
            }
        }
        scanner.close();
    }

    public static void main(String[] args) {
        ConsoleTest app = new ConsoleTest();
        app.startConversation();
    }
}