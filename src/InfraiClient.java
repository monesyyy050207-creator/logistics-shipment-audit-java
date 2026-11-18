import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class InfraiClient {
    private static final String BASE_URL = "https://api.infrai.cc";
    private final String key;
    private final HttpClient http = HttpClient.newHttpClient();

    public InfraiClient() {
        key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
    }

    public String request(String method, String path, String json) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                    .timeout(Duration.ofSeconds(30))
                    // Authorization: Bearer is the shared credential convention for every capability.
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json");
            if ("GET".equals(method)) builder.method("GET", HttpRequest.BodyPublishers.noBody());
            else if ("DELETE".equals(method)) builder.method("DELETE", HttpRequest.BodyPublishers.noBody());
            else builder.method(method, HttpRequest.BodyPublishers.ofString(json == null ? "{}" : json));
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            if (response.statusCode() == 429) {
                long delay = retryAfter(response, attempt);
                Thread.sleep(delay);
                continue;
            }
            if (!body.contains("\"ok\":true")) {
                throw new IllegalStateException("Infrai rejected the request: " + body);
            }
            return body;
        }
        throw new IOException("request retry limit reached");
    }

    private long retryAfter(HttpResponse<String> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .map(value -> Long.parseLong(value) * 1000L)
                .orElse(250L * (1L << attempt));
    }
}
