package selenium.automation.llm;

/**
 * Interface for Language Model services
 */
public interface LLMService {
    /**
     * Get a completion response for a given prompt
     * @param prompt The input prompt
     * @return The model's response
     */
    String getCompletion(String prompt);

    /**
     * Analyze web elements from page source
     * @param pageSource HTML source of the page
     * @return Analysis of the web elements
     */
    String analyzeWebElements(String pageSource);

    /**
     * Validate if a welcome message is present
     * @param pageContent The page content to analyze
     * @return true if welcome message is found
     */
    boolean validateWelcomeMessage(String pageContent);
}
