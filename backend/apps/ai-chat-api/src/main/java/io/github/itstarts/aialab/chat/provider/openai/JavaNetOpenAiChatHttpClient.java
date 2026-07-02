package io.github.itstarts.aialab.chat.provider.openai;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Component
public class JavaNetOpenAiChatHttpClient implements OpenAiChatHttpClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public OpenAiChatHttpResponse post(OpenAiChatHttpRequest request) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(request.url()))
                .timeout(request.timeout())
                .POST(HttpRequest.BodyPublishers.ofString(request.body(), StandardCharsets.UTF_8));
        request.headers().forEach(requestBuilder::header);

        HttpResponse<String> response = httpClient.send(
                requestBuilder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        return new OpenAiChatHttpResponse(response.statusCode(), response.body());
    }
}
