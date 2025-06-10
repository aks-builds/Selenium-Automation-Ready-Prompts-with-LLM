package selenium.automation.config;

import selenium.automation.utils.ConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LLMConfig {
    private static final Logger logger = LoggerFactory.getLogger(LLMConfig.class);
    private static volatile LLMConfig instance;

    private final String baseUrl;
    private final String modelName;
    private final boolean enableFallback;
    private final String provider;
    private final String openAIApiKey;

    private LLMConfig() {
        this.baseUrl = ConfigReader.getProperty("lmstudio.url", "http://127.0.0.1:1234");
        this.modelName = ConfigReader.getProperty("lmstudio.model", "qwen2.5-7b-instruct");
        this.enableFallback = ConfigReader.getBooleanProperty("lmstudio.enable.fallback", true);
        this.provider = ConfigReader.getProperty("llm.provider", "MOCK");
        this.openAIApiKey = ConfigReader.getOpenAIApiKey();

        logger.info("LLM Configuration loaded - Provider: {}, Model: {}", provider, modelName);
        logger.debug("Base URL: {}, Fallback enabled: {}", baseUrl, enableFallback);
    }

    public static LLMConfig getInstance() {
        if (instance == null) {
            synchronized (LLMConfig.class) {
                if (instance == null) {
                    instance = new LLMConfig();
                }
            }
        }
        return instance;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModelName() {
        return modelName;
    }

    public boolean isEnableFallback() {
        return enableFallback;
    }

    public String getProvider() {
        return provider;
    }

    public String getOpenAIApiKey() {
        return openAIApiKey;
    }

    // For testing purposes
    public static void reset() {
        instance = null;
    }
}
