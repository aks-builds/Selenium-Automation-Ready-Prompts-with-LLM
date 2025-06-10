package selenium.automation.llm;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockLLMClient implements LLMService {
    private static final Logger logger = LoggerFactory.getLogger(MockLLMClient.class);

    @Override
    public String getCompletion(String prompt) {
        logger.info("Mock LLM client returning generic completion for prompt");
        return "This is a mock completion response for the prompt: " + prompt;
    }

    @Override
    public String analyzeWebElements(String pageSource) {
        logger.info("Analyzing web elements using mock LLM client");
        try {
            Document doc = Jsoup.parse(pageSource);
            StringBuilder analysis = new StringBuilder();

            // Find search box
            Elements searchInputs = doc.select("input[type=text], input[type=search]");
            if (!searchInputs.isEmpty()) {
                analysis.append("Found search input field. ");
            }

            // Find buttons
            Elements buttons = doc.select("button, input[type=submit]");
            if (!buttons.isEmpty()) {
                analysis.append("Found ").append(buttons.size()).append(" button(s). ");
            }

            // Find links
            Elements links = doc.select("a");
            if (!links.isEmpty()) {
                analysis.append("Found ").append(links.size()).append(" link(s). ");
            }

            // Find forms
            Elements forms = doc.select("form");
            if (!forms.isEmpty()) {
                analysis.append("Found ").append(forms.size()).append(" form(s). ");
            }

            String result = analysis.toString().trim();
            return result.isEmpty() ? "No interactive elements found on the page." : result;
        } catch (Exception e) {
            logger.error("Error analyzing web elements: {}", e.getMessage());
            return "Error analyzing page elements";
        }
    }

    @Override
    public boolean validateWelcomeMessage(String pageContent) {
        logger.info("Validating welcome message using mock LLM client");
        try {
            Document doc = Jsoup.parse(pageContent);
            String text = doc.text().toLowerCase();
            logger.debug("Page text: {}", text);

            // Check for Google
            boolean hasGoogle = text.contains("google");
            logger.debug("Contains 'Google': {}", hasGoogle);
            return hasGoogle;
        } catch (Exception e) {
            logger.error("Error validating welcome message: {}", e.getMessage());
            return false;
        }
    }
}
