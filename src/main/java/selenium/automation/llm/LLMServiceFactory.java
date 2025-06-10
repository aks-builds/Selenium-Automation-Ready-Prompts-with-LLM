package selenium.automation.llm;

import selenium.automation.config.LLMConfig;
import selenium.automation.llm.http.HttpClient;
import selenium.automation.llm.request.LLMRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;

public class LLMServiceFactory {
    private static final Logger logger = LoggerFactory.getLogger(LLMServiceFactory.class);
    private static final int TIMEOUT_SECONDS = 60;

    public static LLMService createService() {
        LLMConfig config = LLMConfig.getInstance();
        String provider = config.getProvider().toUpperCase();
        String apiKey = config.getOpenAIApiKey();

        logger.info("Creating LLM service for provider: {}", provider);

        switch (provider) {
            case "OPENAI":
                return createOpenAIService(apiKey);

            case "LM_STUDIO":
                return createLMStudioService(apiKey);

            case "MOCK":
                return new MockLLMClient();

            default:
                logger.warn("Unknown provider: {}. Using Mock client.", provider);
                return new MockLLMClient();
        }
    }

    private static LLMService createOpenAIService(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("OpenAI API key not provided. Using Mock client.");
            return new MockLLMClient();
        }
        return new OpenAIClient(apiKey);
    }

    private static LLMService createLMStudioService(String apiKey) {
        if (isLMStudioAvailable()) {
            return new LMStudioClient();
        }

        LLMConfig config = LLMConfig.getInstance();
        if (!config.isEnableFallback()) {
            logger.error("LM Studio is not available and fallback is disabled.");
            throw new RuntimeException("LM Studio is not available and fallback is disabled");
        }

        if (apiKey != null && !apiKey.isEmpty()) {
            logger.warn("LM Studio is not available. Falling back to OpenAI.");
            return new OpenAIClient(apiKey);
        } else {
            logger.warn("LM Studio is not available and no OpenAI API key provided. Using Mock client.");
            return new MockLLMClient();
        }
    }

    private static boolean isLMStudioAvailable() {
        LLMConfig config = LLMConfig.getInstance();
        String url = config.getBaseUrl() + "/v1/chat/completions";
        HttpClient client = new HttpClient(TIMEOUT_SECONDS);

        try {
            LLMRequest request = new LLMRequest.Builder(config.getModelName(), "test")
                    .maxTokens(10)
                    .build();

            HttpResponse<String> response = client.post(url, request.toJson());

            boolean isAvailable = response.statusCode() == 200;
            logger.info("LM Studio availability check - Available: {}", isAvailable);
            return isAvailable;
        } catch (Exception e) {
            logger.error("LM Studio availability check failed", e);
            logger.warn("LM Studio is not available at {}: {}", url, e.getMessage());
            return false;
        }
    }
}
