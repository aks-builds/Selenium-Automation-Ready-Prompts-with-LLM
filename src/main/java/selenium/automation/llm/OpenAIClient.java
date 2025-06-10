package selenium.automation.llm;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class OpenAIClient implements LLMService {
    private static final Logger logger = LoggerFactory.getLogger(OpenAIClient.class);
    private final OpenAiService service;
    private final MockLLMClient mockClient;
    private static final String MODEL = "gpt-3.5-turbo";
    private static final int MAX_RETRIES = 2;
    private static final int TIMEOUT_SECONDS = 30;
    private boolean useMockClient = false;

    public OpenAIClient(String apiKey) {
        logger.info("Initializing OpenAI client");
        this.mockClient = new MockLLMClient();

        OpenAiService tempService = null;
        if (apiKey == null || apiKey.trim().isEmpty() || "${OPENAI_API_KEY}".equals(apiKey)) {
            logger.warn("Invalid OpenAI API key provided. Falling back to mock client.");
            this.useMockClient = true;
        } else {
            try {
                tempService = new OpenAiService(apiKey, Duration.ofSeconds(TIMEOUT_SECONDS));
                logger.info("OpenAI client initialized successfully");
            } catch (Exception e) {
                logger.error("Failed to initialize OpenAI client: {}. Falling back to mock client.", e.getMessage());
                this.useMockClient = true;
            }
        }
        this.service = tempService;
    }

    public String getCompletion(String prompt) {
        if (useMockClient) {
            logger.info("Using mock client for completion");
            return mockClient.analyzeWebElements(prompt);
        }

        logger.info("Getting completion for prompt");
        int retries = 0;
        Exception lastException = null;

        while (retries < MAX_RETRIES) {
            try {
                List<ChatMessage> messages = new ArrayList<>();
                messages.add(new ChatMessage("user", prompt));

                logger.debug("Sending request to OpenAI with model: {}", MODEL);
                ChatCompletionRequest request = ChatCompletionRequest.builder()
                        .model(MODEL)
                        .messages(messages)
                        .maxTokens(500)
                        .temperature(0.7)
                        .build();

                String response = service.createChatCompletion(request)
                        .getChoices().get(0).getMessage().getContent();
                logger.info("Successfully received response from OpenAI");
                return response;
            } catch (Exception e) {
                lastException = e;
                retries++;
                logger.warn("Attempt {} failed: {}", retries, e.getMessage());
                if (retries < MAX_RETRIES) {
                    try {
                        Thread.sleep(1000 * retries);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    logger.warn("All OpenAI attempts failed. Switching to mock client.");
                    useMockClient = true;
                    return mockClient.analyzeWebElements(prompt);
                }
            }
        }

        logger.error("Failed to get completion after {} attempts", MAX_RETRIES, lastException);
        throw new RuntimeException("Failed to get completion from OpenAI after " + MAX_RETRIES + " attempts", lastException);
    }

    public String analyzeWebElements(String pageSource) {
        logger.info("Analyzing web elements");
        try {
            if (useMockClient) {
                return mockClient.analyzeWebElements(pageSource);
            }

            // Truncate page source if too long
            String truncatedSource = pageSource.length() > 4000 ?
                    pageSource.substring(0, 4000) + "... (truncated)" : pageSource;

            String prompt = String.format(
                    "Analyze the following HTML and identify the main interactive elements (buttons, links, forms) and their purpose. " +
                            "Keep the response concise and focus on key elements: %s",
                    truncatedSource
            );

            try {
                return getCompletion(prompt);
            } catch (Exception e) {
                logger.warn("OpenAI request failed, falling back to mock client");
                useMockClient = true;
                return mockClient.analyzeWebElements(pageSource);
            }
        } catch (Exception e) {
            logger.error("Failed to analyze web elements: {}", e.getMessage());
            throw new RuntimeException("Failed to analyze web elements", e);
        }
    }

    public boolean validateWelcomeMessage(String pageContent) {
        logger.info("Validating welcome message");
        try {
            if (useMockClient) {
                return mockClient.validateWelcomeMessage(pageContent);
            }

            // Truncate page content if too long
            String truncatedContent = pageContent.length() > 4000 ?
                    pageContent.substring(0, 4000) + "... (truncated)" : pageContent;

            String prompt = String.format(
                    "Analyze this page content and determine if it contains a welcome message or greeting. " +
                            "Respond with 'yes' or 'no' and briefly explain why: %s",
                    truncatedContent
            );

            try {
                String response = getCompletion(prompt);
                logger.info("Welcome message validation response: {}", response);
                return response.toLowerCase().contains("yes");
            } catch (Exception e) {
                logger.warn("OpenAI request failed, falling back to mock client");
                useMockClient = true;
                return mockClient.validateWelcomeMessage(pageContent);
            }
        } catch (Exception e) {
            logger.error("Failed to validate welcome message: {}", e.getMessage());
            throw new RuntimeException("Failed to validate welcome message", e);
        }
    }
}
