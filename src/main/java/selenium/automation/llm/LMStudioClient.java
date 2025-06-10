package selenium.automation.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import selenium.automation.config.LLMConfig;
import selenium.automation.llm.http.HttpClient;
import selenium.automation.llm.request.LLMRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

public class LMStudioClient implements LLMService {
    private static final Logger logger = LoggerFactory.getLogger(LMStudioClient.class);
    private static final int TIMEOUT_SECONDS = 60;
    private static final int MAX_RETRIES = 2;

    private final String baseUrl;
    private final String modelName;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final LLMService fallbackClient;
    private final boolean enableFallback;
    private boolean useFallback = false;

    public LMStudioClient() {
        LLMConfig config = LLMConfig.getInstance();
        this.baseUrl = config.getBaseUrl();
        this.modelName = config.getModelName();
        this.enableFallback = config.isEnableFallback();

        this.objectMapper = new ObjectMapper();
        this.httpClient = new HttpClient(TIMEOUT_SECONDS);
        this.fallbackClient = new MockLLMClient();

        validateConnection();
    }

    private void validateConnection() {
        try {
            LLMRequest request = new LLMRequest.Builder(modelName, "test")
                    .maxTokens(10)
                    .build();

            String url = baseUrl + "/v1/chat/completions";
            HttpResponse<String> response = httpClient.post(url, request.toJson());

            if (response.statusCode() == 200) {
                logger.info("Successfully connected to LM Studio at {} using model {}", baseUrl, modelName);
            } else {
                throw new RuntimeException("LM Studio returned status code: " + response.statusCode());
            }
        } catch (Exception e) {
            handleConnectionError(e);
        }
    }

    private void handleConnectionError(Exception e) {
        String message = String.format("Could not connect to LM Studio: %s. Make sure the server is running at %s.",
                e.getMessage(), baseUrl);
        if (enableFallback) {
            logger.warn(message + " Falling back to mock client.");
            useFallback = true;
        } else {
            logger.error(message + " Fallback is disabled.");
            throw new RuntimeException("Failed to connect to LM Studio and fallback is disabled", e);
        }
    }

    private String makeRequest(String prompt) {
        if (useFallback) {
            return handleFallback(prompt);
        }

        int retries = 0;
        Exception lastException = null;

        while (retries < MAX_RETRIES) {
            try {
                LLMRequest request = new LLMRequest.Builder(modelName, prompt).build();
                String url = baseUrl + "/v1/chat/completions";

                HttpResponse<String> response = httpClient.post(url, request.toJson());

                if (response.statusCode() == 200) {
                    return extractContent(response.body());
                } else {
                    throw new RuntimeException("LM Studio returned status code: " + response.statusCode());
                }
            } catch (Exception e) {
                lastException = e;
                retries++;
                logger.error("Request attempt {} failed", retries, e);
                if (retries < MAX_RETRIES) {
                    handleRetry(retries);
                }
            }
        }

        return handleMaxRetriesExceeded(prompt, lastException);
    }

    private String handleFallback(String prompt) {
        if (!enableFallback) {
            throw new RuntimeException("LM Studio is unavailable and fallback is disabled");
        }
        logger.info("Using fallback mock client");
        return fallbackClient.getCompletion(prompt);
    }

    private void handleRetry(int retryCount) {
        try {
            TimeUnit.SECONDS.sleep(retryCount);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private String handleMaxRetriesExceeded(String prompt, Exception lastException) {
        String message = String.format("Failed to get completion from LM Studio after %d attempts", MAX_RETRIES);
        if (enableFallback) {
            logger.warn(message + ". Falling back to mock client.");
            useFallback = true;
            return fallbackClient.getCompletion(prompt);
        } else {
            logger.error(message + ". Fallback is disabled.");
            throw new RuntimeException(message, lastException);
        }
    }

    private String extractContent(String responseBody) {
        try {
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            String content = jsonResponse.path("choices").path(0).path("message").path("content").asText();
            logger.debug("Extracted content: {}", content);
            return content;
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract content from response", e);
        }
    }

    @Override
    public String getCompletion(String prompt) {
        logger.info("Getting completion from LM Studio");
        return makeRequest(prompt);
    }

    @Override
    public String analyzeWebElements(String pageSource) {
        logger.info("Analyzing web elements using LM Studio");
        String prompt = String.format(
                "Analyze the following HTML and identify the main interactive elements (buttons, links, forms) and their purpose. " +
                        "Keep the response concise and focus on key elements: %s",
                pageSource.length() > 4000 ? pageSource.substring(0, 4000) + "... (truncated)" : pageSource
        );
        return makeRequest(prompt);
    }

    @Override
    public boolean validateWelcomeMessage(String pageContent) {
        logger.info("Validating welcome message using LM Studio");
        String prompt = String.format(
                "Check if this page content contains the word 'Google'. " +
                        "Respond with 'yes' if it contains 'Google', or 'no' if it doesn't: %s",
                pageContent.length() > 4000 ? pageContent.substring(0, 4000) + "... (truncated)" : pageContent
        );
        String response = makeRequest(prompt);
        logger.info("Welcome message validation response: {}", response);
        return response.toLowerCase().contains("yes");
    }
}
