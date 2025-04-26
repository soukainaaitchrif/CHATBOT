package com.example.chatbottest.config;

import io.github.cdimascio.dotenv.Dotenv;

public class ConfigLoader {
    private static Dotenv dotenv = Dotenv.load();
    private static final String OLLAMA_BASE_URL = dotenv.get("OLLAMA_BASE_URL");
    private static final String OLLAMA_MODEL_NAME = dotenv.get("OLLAMA_MODEL_NAME");
    private static final String DB_HOST = dotenv.get("DB_HOST");
    private static final int DB_PORT = Integer.parseInt(dotenv.get("DB_PORT"));
    private static final String DB_NAME = dotenv.get("DB_NAME");
    private static final String DB_USER = dotenv.get("DB_USER");
    private static final String DB_PASSWORD = dotenv.get("DB_PASSWORD");
    private static final String DB_TABLE = dotenv.get("DB_TABLE");

    public static String getOllamaBaseUrl() {
        return OLLAMA_BASE_URL;
    }
    public static String getOllamaModelName() {
        return OLLAMA_MODEL_NAME;
    }
    public static String getDbHost() {
        return DB_HOST;
    }
    public static int getDbPort() {
        return DB_PORT;
    }
    public static String getDbName() {
        return DB_NAME;
    }
    public static String getDbUser() {
        return DB_USER;
    }
    public static String getDbPassword() {
        return DB_PASSWORD;
    }
    public static String getDbTable() {
        return DB_TABLE;
    }
}
