package selenium.automation.llm.request;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LLMRequest {
    private final String model;
    private final String prompt;
    private final double temperature;
    private final int maxTokens;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private LLMRequest(Builder builder) {
        this.model = builder.model;
        this.prompt = builder.prompt;
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
    }

    public String toJson() {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", "user");
        message.put("content", prompt);

        ArrayNode messages = objectMapper.createArrayNode();
        messages.add(message);

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.set("messages", messages);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);

        return requestBody.toString();
    }

    public static class Builder {
        private String model;
        private String prompt;
        private double temperature = 0.7;  // default value
        private int maxTokens = 500;      // default value

        public Builder(String model, String prompt) {
            this.model = model;
            this.prompt = prompt;
        }

        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public LLMRequest build() {
            return new LLMRequest(this);
        }
    }
}
