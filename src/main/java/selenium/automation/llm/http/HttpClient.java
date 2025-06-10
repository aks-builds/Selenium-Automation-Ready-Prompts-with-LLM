package selenium.automation.llm.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClient {
    private static final Logger logger = LoggerFactory.getLogger(HttpClient.class);
    private final java.net.http.HttpClient client;
    private final int timeoutSeconds;

    public HttpClient(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
        this.client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .build();
    }

    public HttpResponse<String> post(String url, String jsonBody) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        logger.debug("Making request to URL: {}", url);
        logger.debug("Request body: {}", jsonBody);

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        logger.debug("Response status: {}", response.statusCode());
        logger.debug("Response body: {}", response.body());

        return response;
    }
}
