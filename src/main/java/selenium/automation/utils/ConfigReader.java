package selenium.automation.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigReader.class);
    private static Properties properties;

    public static void init() {
        properties = new Properties();  // Always reload properties
        try (InputStream input = ConfigReader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("Unable to find config.properties");
            }
            properties.load(input);
            logger.info("Configuration loaded successfully");
            logger.debug("All loaded properties: {}", properties);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    public static String getProperty(String key) {
        return getProperty(key, null);
    }

    public static String getProperty(String key, String defaultValue) {
        if (properties == null) {
            init();
        }

        // First check system properties
        String value = System.getProperty(key);
        if (value != null) {
            logger.debug("Found system property for {}: {}", key, value);
            return value;
        }

        // Then check environment variables
        String envKey = key.toUpperCase().replace('.', '_');
        value = System.getenv(envKey);
        if (value != null) {
            logger.debug("Found environment variable for {} ({}): {}", key, envKey, value);
            return value;
        }

        // Finally check properties file
        value = properties.getProperty(key, defaultValue);
        logger.debug("Using value from properties file for {}: {}", key, value);
        return value;
    }

    public static String getLLMProvider() {
        String provider = getProperty("llm.provider", "MOCK").toUpperCase();
        logger.debug("Using LLM provider: {}", provider);
        return provider;
    }

    public static String getOpenAIApiKey() {
        // Try environment variable first
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey != null && !apiKey.isEmpty()) {
            logger.debug("Found OpenAI API key in environment variables");
            return apiKey;
        }

        // Then try system property
        apiKey = System.getProperty("openai.api.key");
        if (apiKey != null && !apiKey.isEmpty()) {
            logger.debug("Found OpenAI API key in system properties");
            return apiKey;
        }

        // Finally try config file
        apiKey = getProperty("openai.api.key");
        if (apiKey != null && !apiKey.isEmpty() && !apiKey.startsWith("${")) {
            logger.debug("Found OpenAI API key in config file");
            return apiKey;
        }

        logger.warn("No OpenAI API key found in environment variables, system properties, or config file");
        return null;
    }

    public static int getIntProperty(String key, int defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer value for {}: {}. Using default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            logger.debug("No value found for boolean property {}, using default: {}", key, defaultValue);
            return defaultValue;
        }
        boolean result = Boolean.parseBoolean(value);
        logger.debug("Boolean property {} = {} (from value: {})", key, result, value);
        return result;
    }
}
