package com.example.chatbottest;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ChatBot extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ChatBot.class.getResource("/com/example/chatbottest/conversation-view.fxml"));

        Scene scene = new Scene(fxmlLoader.load(), 850, 600);
        scene.getStylesheets().add(ChatBot.class.getResource("/com/example/chatbottest/styleConversation.css").toExternalForm());
        stage.setTitle("ChatBot");
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}