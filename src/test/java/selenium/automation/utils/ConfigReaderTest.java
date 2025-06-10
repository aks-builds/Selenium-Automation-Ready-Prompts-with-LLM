package selenium.automation.utils;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ConfigReaderTest {

    @BeforeMethod
    void setUp() {
        // Force reload of properties before each test
        ConfigReader.init();
    }

    @Test
    void testLMStudioModelConfiguration() {
        String model = ConfigReader.getProperty("lmstudio.model");
        assertNotNull(model, "LM Studio model should be configured");
        assertEquals(model, "qwen2.5-7b-instruct", "LM Studio model should match config.properties");
    }
}
