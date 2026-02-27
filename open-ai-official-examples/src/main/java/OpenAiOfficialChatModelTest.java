import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openaiofficial.OpenAiOfficialChatModel;
import dev.langchain4j.model.openaiofficial.OpenAiOfficialStreamingChatModel;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiOfficialChatModelTest {

//    public static final String BASE_URL = System.getenv("OPENROUTER_BASE_URL");
//    public static final String API_KEY = System.getenv("OPENROUTER_API_KEY");
//    public static final String MODEL_NAME = "z-ai/glm-4.5-air:free";

//    public static final String BASE_URL = System.getenv("ZAI_BASE_URL");
//    public static final String API_KEY = System.getenv("ZAI_API_KEY");
//    public static final String MODEL_NAME = "GLM-4.7-Flash";

    public static final String BASE_URL = System.getenv("OPENAI_BASE_URL");
    public static final String API_KEY = System.getenv("OPENAI_API_KEY");
    public static final String MODEL_NAME = "Kimi-K2";

    @Test
    void model_list_openrouter() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://openrouter.ai/api/v1/models"))
                .GET()
                .build();
        String response = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);
        JsonNode data = root.get("data");
        List<String> freeModels = new ArrayList<>();
        if (data.isArray()) {
            for (JsonNode model : data) {
                String id = model.get("id").asText();
                // 关键点：OpenRouter 的免费模型通常以 :free 结尾
                if (id.endsWith(":free")) {
                    freeModels.add(id);
                }
            }
        }
        System.out.println("--- 当前可用免费模型 ---");
        freeModels.forEach(System.out::println);
    }

    @Test
    void model_list_openai() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(System.getenv("OPENAI_BASE_URL")).resolve("models"))
                .GET()
                .build();
        String response = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);
        JsonNode data = root.get("data");
        List<String> freeModels = new ArrayList<>();
        if (data.isArray()) {
            for (JsonNode model : data) {
                String id = model.get("id").asText();
                // 关键点：OpenRouter 的免费模型通常以 :free 结尾
                if (id.endsWith(":free")) {
                    freeModels.add(id);
                }
            }
        }
        System.out.println("--- 当前可用免费模型 ---");
        freeModels.forEach(System.out::println);
    }

    @Test
    void streaming_example() {

        StreamingChatModel model = OpenAiOfficialStreamingChatModel.builder()
                .baseUrl(BASE_URL)
                .apiKey(API_KEY)
                .modelName(MODEL_NAME)
                .build();

        String userMessage = "Write a 100-word poem about Java and AI";

        CompletableFuture<ChatResponse> futureResponse = new CompletableFuture<>();

        model.chat(userMessage, new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String partialResponse) {
                System.out.print(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                futureResponse.complete(completeResponse);
            }

            @Override
            public void onError(Throwable error) {
                futureResponse.completeExceptionally(error);
            }
        });

        futureResponse.join();
    }

    /**
     * If you have Ollama running locally,
     * please set the OLLAMA_BASE_URL environment variable (e.g., http://localhost:11434).
     * If you do not set the OLLAMA_BASE_URL environment variable,
     * Testcontainers will download and start Ollama Docker container.
     * It might take a few minutes.
     */

    @Test
    void simple_example() {

        ChatModel chatModel = OpenAiOfficialChatModel.builder()
                .baseUrl(BASE_URL)
                .apiKey(API_KEY)
                .modelName(MODEL_NAME)
                .build();

        String answer = chatModel.chat("Provide 3 short bullet points explaining why Java is awesome");
        System.out.println(answer);

        assertThat(answer).isNotBlank();
    }

    @Test
    void json_schema_with_AI_Service_example() {

        record Person(String name, int age) {
        }

        interface PersonExtractor {

            Person extractPersonFrom(String text);
        }

        ChatModel chatModel = OpenAiOfficialChatModel.builder()
                .baseUrl(BASE_URL)
                .modelName(MODEL_NAME)
                .temperature(0.0)
                .build();

        PersonExtractor personExtractor = AiServices.create(PersonExtractor.class, chatModel);

        Person person = personExtractor.extractPersonFrom("John Doe is 42 years old");
        System.out.println(person);

        assertThat(person).isEqualTo(new Person("John Doe", 42));
    }

    @Test
    void json_schema_with_low_level_chat_api_example() {

        ChatModel chatModel = OpenAiOfficialChatModel.builder()
                .baseUrl(BASE_URL)
                .modelName(MODEL_NAME)
                .temperature(0.0)
                .build();

        ResponseFormat responseFormat = ResponseFormat.builder()
                .type(ResponseFormatType.JSON)
                .jsonSchema(JsonSchema.builder()
                        .rootElement(JsonObjectSchema.builder()
                                .addStringProperty("name")
                                .addIntegerProperty("age")
                                .build())
                        .build())
                .build();

        ChatRequestParameters parameters = ChatRequestParameters.builder()
                .responseFormat(responseFormat)
                .build();

        ChatRequest chatRequest = ChatRequest.builder()
                .messages(UserMessage.from("John Doe is 42 years old"))
                .parameters(parameters)
                .build();

        ChatResponse chatResponse = chatModel.chat(chatRequest);
        System.out.println(chatResponse);

        assertThat(toMap(chatResponse.aiMessage().text())).isEqualTo(Map.of("name", "John Doe", "age", 42));
    }

    @Test
    void json_schema_with_low_level_model_builder_example() {

        ChatModel chatModel = OpenAiOfficialChatModel.builder()
                .baseUrl(BASE_URL)
                .modelName(MODEL_NAME)
                .temperature(0.0)
                .build();

        String json = chatModel.chat("Extract: John Doe is 42 years old");
        System.out.println(json);

        assertThat(toMap(json)).isEqualTo(Map.of("name", "John Doe", "age", 42));
    }

    @Test
    void json_mode_with_low_level_model_builder_example() {

        ChatModel chatModel = OpenAiOfficialChatModel.builder()
                .baseUrl(BASE_URL)
                .modelName(MODEL_NAME)
                .temperature(0.0)
                .build();

        String json = chatModel.chat("Give me a JSON object with 2 fields: name and age of a John Doe, 42");
        System.out.println(json);

        assertThat(toMap(json)).isEqualTo(Map.of("name", "John Doe", "age", 42));
    }

    private static Map<String, Object> toMap(String json) {
        try {
            return new ObjectMapper().readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
