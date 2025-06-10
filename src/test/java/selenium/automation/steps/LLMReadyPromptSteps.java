package selenium.automation.steps;

import selenium.automation.llm.LLMService;
import selenium.automation.llm.LLMServiceFactory;
import selenium.automation.utils.ConfigReader;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.time.Duration;

public class LLMReadyPromptSteps {
    private static final Logger logger = LoggerFactory.getLogger(LLMReadyPromptSteps.class);
    private WebDriver driver;
    private String pageSource;
    private String analysisResult;
    private final LLMService llmService;
    private WebDriverWait wait;

    public LLMReadyPromptSteps() {
        ConfigReader.init();
        llmService = LLMServiceFactory.createService();
    }

    @Before
    public void setup() {
        ChromeOptions options = new ChromeOptions();
        if (Boolean.parseBoolean(ConfigReader.getProperty("headless", "false"))) {
            options.addArguments("--headless");
        }
        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(
                Integer.parseInt(ConfigReader.getProperty("explicit.wait", "10"))
        ));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(
                Integer.parseInt(ConfigReader.getProperty("implicit.wait", "10"))
        ));
    }

    @After
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Given("I open the browser and navigate to {string}")
    public void iOpenTheBrowserAndNavigateTo(String url) {
        logger.info("Navigating to URL: {}", url);
        try {
            driver.get(url);
            pageSource = driver.getPageSource();
            // Wait for page to be fully loaded
            Thread.sleep(2000); // Small delay to ensure page is loaded
        } catch (Exception e) {
            logger.error("Failed to navigate to URL: {}", e.getMessage());
            throw new RuntimeException("Failed to navigate to URL", e);
        }
    }

    @When("I analyze the page elements using LLM")
    public void iAnalyzeThePageElementsUsingLLM() {
        logger.info("Analyzing page elements using LLM");
        try {
            // Wait for dynamic content to load
            Thread.sleep(2000);
            analysisResult = llmService.analyzeWebElements(pageSource);
            logger.info("Page analysis result: {}", analysisResult);
        } catch (Exception e) {
            logger.error("Failed to analyze page elements: {}", e.getMessage());
            throw new RuntimeException("Failed to analyze page elements", e);
        }
    }

    @Then("I should validate the page content using LLM")
    public void iShouldValidateThePageContentUsingLLM() {
        logger.info("Validating page content using LLM analysis");
        try {
            Assert.assertNotNull(analysisResult, "Page analysis should not be null");
            Assert.assertTrue(analysisResult.length() > 0, "Page analysis should not be empty");
            logger.info("Page content validation successful");
        } catch (Exception e) {
            logger.error("Failed to validate page content: {}", e.getMessage());
            throw new RuntimeException("Failed to validate page content", e);
        }
    }
}
