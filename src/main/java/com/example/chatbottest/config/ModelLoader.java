package com.example.chatbottest.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;

import java.time.Duration;

public class ModelLoader {
    private static final int EMBEDDING_DIMENSION = 384;
    private static final EmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
            .baseUrl(ConfigLoader.getOllamaBaseUrl())
            .modelName(ConfigLoader.getOllamaModelName())
            .build();
    private static final EmbeddingStore<TextSegment> embeddingStore = PgVectorEmbeddingStore.builder()
            .host(ConfigLoader.getDbHost())
            .port(ConfigLoader.getDbPort())
            .database(ConfigLoader.getDbName())
            .user(ConfigLoader.getDbUser())
            .password(ConfigLoader.getDbPassword())
            .table(ConfigLoader.getDbTable())
            .dimension(EMBEDDING_DIMENSION)
            .createTable(true)
            .build();

    //Use the Ollama model
//        this.model = OllamaChatModel.builder()
//                .baseUrl(dotenv.get("OLLAMA_BASE_URL"))
//                .modelName(dotenv.get("OLLAMA_MODEL_CHAT"))
//                .build();
    private static final ChatLanguageModel model = OpenAiChatModel.builder()
            .apiKey("demo")
            .modelName(OpenAiChatModelName.GPT_4_O_MINI)
            .timeout(Duration.ofSeconds(15))
            .temperature(0.7)
            .build();
    public static EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }

    public static EmbeddingStore<TextSegment> getEmbeddingStore() {
        return embeddingStore;
    }

    public static ChatLanguageModel getChatModel() {
        return model;
    }

}
