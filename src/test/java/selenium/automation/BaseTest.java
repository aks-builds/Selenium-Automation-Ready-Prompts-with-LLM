package selenium.automation;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.*;
import org.json.JSONObject;
import okhttp3.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import selenium.automation.factory.WebDriverFactory;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.util.Arrays;

public class BaseTest {
    private static final Logger logger = LoggerFactory.getLogger(BaseTest.class);
    protected WebDriver driver;
    //private static final String OPENAI_API_KEY = "";
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final MediaType JSON = MediaType.parse("application/json");

    private final OkHttpClient client = new OkHttpClient();

    @BeforeMethod
    @Parameters({"browser"})
    public void setUp(String browser) {
        try {
            logger.info("Setting up test with browser: {}", browser);

            // Setup WebDriverManager
            setupWebDriverManager(browser);

            // Create WebDriver
            driver = WebDriverFactory.createDriver(browser);

            // Log success
            logger.info("WebDriver initialized successfully: {}", driver.getClass().getSimpleName());
            logSystemInfo();

        } catch (Exception e) {
            logger.error("Setup failed: {}", e.getMessage());
            throw new RuntimeException("Setup failed", e);
        }
    }

    private void setupWebDriverManager(String browser) {
        try {
            switch (browser.toLowerCase()) {
                case "chrome":
                    WebDriverManager.chromedriver().setup();
                    logger.info("ChromeDriver binary path: {}", WebDriverManager.chromedriver().getDownloadedDriverPath());
                    break;
                case "firefox":
                    WebDriverManager.firefoxdriver().setup();
                    logger.info("FirefoxDriver binary path: {}", WebDriverManager.firefoxdriver().getDownloadedDriverPath());
                    break;
                case "edge":
                    WebDriverManager.edgedriver().setup();
                    logger.info("EdgeDriver binary path: {}", WebDriverManager.edgedriver().getDownloadedDriverPath());
                    break;
                default:
                    throw new RuntimeException(String.format("Unsupported browser type: %s", browser));
            }
        } catch (Exception e) {
            logger.error("Failed to setup WebDriverManager: {}", e.getMessage());
            throw new RuntimeException("WebDriverManager setup failed", e);
        }
    }

    private void logSystemInfo() {
        logger.info("System Information:");
        logger.info("OS: {} {}", System.getProperty("os.name"), System.getProperty("os.version"));
        logger.info("Java Version: {}", System.getProperty("java.version"));
        logger.info("Browser: {}", driver.getClass().getSimpleName());

        // Log browser-specific information
        if (driver instanceof ChromeDriver) {
            logger.info("Chrome Version: {}", ((ChromeDriver) driver).getCapabilities().getBrowserVersion());
        } else if (driver instanceof FirefoxDriver) {
            logger.info("Firefox Version: {}", ((FirefoxDriver) driver).getCapabilities().getBrowserVersion());
        }
    }

    @AfterMethod
    public void captureFailure(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE) {
            JSONObject failureDetails = new JSONObject();
            failureDetails.put("testName", result.getName());
            failureDetails.put("errorMessage", result.getThrowable().getMessage());
            failureDetails.put("stackTrace", Arrays.toString(result.getThrowable().getStackTrace()));

            sendToLLM(failureDetails);
        }
    }

    public void sendToLLM(JSONObject failureDetails)
    {
        try {
            // Construct prompt using failure details
            String prompt = buildPromptFromFailure(failureDetails);

            // Build request body
            JSONObject body = new JSONObject();
            body.put("model", "gpt-4");
            body.put("messages", new org.json.JSONArray()
                    .put(new JSONObject()
                            .put("role", "user")
                            .put("content", prompt)
                    ));
            body.put("max_tokens", 300);

            Request request = new Request.Builder()
                    .url(OPENAI_API_URL)
                    //.addHeader("Authorization", "Bearer " + OPENAI_API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(JSON, body.toString()))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONObject result = new JSONObject(responseBody);
                    String reply = result.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");
                    System.out.println("🧠 LLM Suggestion:\n" + reply);
                } else {
                    System.err.println("❌ LLM call failed: " + response);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String buildPromptFromFailure(JSONObject failureDetails) {
        return String.format("""
                A Selenium test has failed with the following details:

                Test Name: %s
                Error Message: %s
                Stack Trace: %s
                Screenshot Path (if available): %s

                Please analyze the failure and suggest a possible root cause and fix.
                """,
                failureDetails.optString("testName", "Unknown"),
                failureDetails.optString("errorMessage", "N/A"),
                failureDetails.optString("stackTrace", "N/A"),
                failureDetails.optString("screenshot", "N/A")
        );
    }


    public void tearDown() {
        if (driver != null) {
            try {
                // Log final state
                logger.info("Quitting WebDriver");
                logger.info("Final URL before quit: {}", driver.getCurrentUrl());

                // Quit WebDriver
                driver.quit();
                driver = null;
                logger.info("WebDriver quit successfully");


            } catch (Exception e) {
                logger.error("Error during WebDriver cleanup: {}", e.getMessage());
                // Force cleanup if normal quit fails
                try {
                    if (driver != null) {
                        driver.quit();
                    }
                } catch (Exception ex) {
                    logger.error("Force quit failed: {}", ex.getMessage());
                } finally {
                    driver = null;
                }
            }
        }
    }
}
