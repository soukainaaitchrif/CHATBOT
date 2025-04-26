package com.example.chatbottest.controller;

import com.example.chatbottest.service.ChatService;
import com.example.chatbottest.service.DocumentService;
import com.example.chatbottest.service.EmbeddingService;
import com.example.chatbottest.service.LLMService;
import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.control.ScrollPane;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
public class ConversationController implements Initializable {

    @FXML
    private Label chatTitle;

    @FXML
    private VBox messageContainer;

    @FXML
    private ListView listConversations;

    @FXML
    private ScrollPane displayMessages;

    @FXML
    private Button addDoc;

    @FXML
    private Button docButton2;

    @FXML
    private Button sendButton;

    @FXML
    private TextField messageField;

    private LLMService llmService;
    private ChatService chatService;
    private ArrayList<Map<String, String>> messagesList = new ArrayList<>();


    private void addMessage(String message, String sender) {
        HBox messageBox = new HBox();
        Text text = new Text();
        if (message.length() > 100) {
            text.wrappingWidthProperty().bind(displayMessages.widthProperty().multiply(0.7));
        }
        HBox textBox = new HBox();
        textBox.getChildren().add(text);
        textBox.setAlignment(Pos.CENTER);

        if (sender.equals("bot")) {
            textBox.getStyleClass().add("bot-message");
            messageBox.getChildren().add(textBox);
            messageBox.setAlignment(Pos.CENTER_LEFT);
        } else if(sender.equals("user")){
            textBox.getStyleClass().add("user-message");
            messageBox.getChildren().add(textBox);
            messageBox.setAlignment(Pos.CENTER_RIGHT);
        }else {
            textBox.getStyleClass().add("placeholder-message");
            messageBox.getChildren().add(textBox);
            messageBox.setAlignment(Pos.CENTER_LEFT);
        }
        messageBox.setSpacing(10);
        messageContainer.getChildren().add(messageBox);

        new Thread(() -> {
            StringBuffer stringBuffer = new StringBuffer();
            String[] words = message.split(" ");
            for (String word : words) {
                stringBuffer.append(word).append(" ");
                Platform.runLater(() -> text.setText(stringBuffer.toString()));
                try {
                    Thread.sleep(100); // Adjust the delay as needed
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void displayWord(String word) {
        Platform.runLater(() -> {
            if (messageContainer.getChildren().isEmpty() || !(messageContainer.getChildren().get(messageContainer.getChildren().size() - 1) instanceof HBox)) {
                addMessage("", "bot");
            }
            HBox lastMessageBox = (HBox) messageContainer.getChildren().get(messageContainer.getChildren().size() - 1);
            Text text = (Text) ((HBox) lastMessageBox.getChildren().get(0)).getChildren().get(0);
            text.setText(text.getText() + word + " ");
        });
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Dotenv dotenv = Dotenv.load();
        EmbeddingService embeddingService = new EmbeddingService(dotenv);
        this.chatService = new ChatService(dotenv, embeddingService);
        populateMessagesList();
        for (Map<String, String> message : messagesList) {
            addMessage(message.get("message"), message.get("sender"));
        }

        messageContainer.setPadding(new javafx.geometry.Insets(10));
        VBox.setVgrow(displayMessages, Priority.ALWAYS);


        messageField.setPromptText("Type your message here...");
        HBox.setHgrow(messageField, Priority.ALWAYS);
    }

    private void populateMessagesList() {
        Map<String, String> message1 = new HashMap<>();
        message1.put("message", "Hello, how can I help you?");
        message1.put("send_At", "2021-09-01T12:00:00Z");
        message1.put("sender", "bot");
        messagesList.add(message1);
    }

    @FXML
    private void handleDocButton1Action() {
        // Handle Doc button 1 action
    }

    @FXML
    private void handleAddDocument() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Document");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            try (PDDocument document = PDDocument.load(selectedFile)) {
                PDFTextStripper pdfStripper = new PDFTextStripper();
                String documentText = pdfStripper.getText(document);
                Dotenv dotenv = Dotenv.load();
                EmbeddingService embeddingService = new EmbeddingService(dotenv);
                DocumentService documentService = new DocumentService(embeddingService);
                documentService.addDocument(documentText);
                addMessage("Document uploaded and added successfully.", "bot");
            } catch (Exception e) {
                addMessage("Failed to upload and add document: " + e.getMessage(), "bot");
            }
        } else {
            addMessage("No document selected.", "bot");
        }
    }

    @FXML
    private void handleSendButtonAction() {
        String userMessage = messageField.getText();
        if (userMessage != null && !userMessage.trim().isEmpty()) {
            // Add user message to UI
            addMessage(userMessage, "user");
            messageField.clear();

            // Send the message to the LLMService and get the response
            Task<String> chatTask = new Task<>() {
                @Override
                protected String call() throws Exception {
                    Thread.sleep(500);
                    return chatService.processUserInput(userMessage);
                }
            };

            chatTask.setOnSucceeded(event -> {
                stopThinking();
                messageContainer.getChildren().remove(messageContainer.getChildren().size() - 1);
                addMessage(chatTask.getValue(), "bot");
            });

            Platform.runLater(() -> {
                showThinking();
                new Thread(chatTask).start();
            });
        }
    }

    public void showThinking() {
        Platform.runLater(() -> {
            StringBuffer processingMessage = new StringBuffer("Thinking");
            addMessage(processingMessage.toString(), "");
            HBox lastMessageBox = (HBox) messageContainer.getChildren().get(messageContainer.getChildren().size() - 1);
            Text text = (Text) ((HBox) lastMessageBox.getChildren().get(0)).getChildren().get(0);

            Thread thinkingThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    for (int i = 0; i < 3; i++) {
                        processingMessage.append(".");
                        String updatedMessage = processingMessage.toString();
                        Platform.runLater(() -> text.setText(updatedMessage));
                        try {
                            Thread.sleep(250);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    processingMessage.setLength(8); // Reset to "Thinking"
                }
            });
            thinkingThread.start();

            // Store the thread reference to interrupt it later
            messageContainer.setUserData(thinkingThread);
        });
    }

    public void stopThinking() {
        Thread thinkingThread = (Thread) messageContainer.getUserData();
        if (thinkingThread != null) {
            thinkingThread.interrupt();
        }
    }
}