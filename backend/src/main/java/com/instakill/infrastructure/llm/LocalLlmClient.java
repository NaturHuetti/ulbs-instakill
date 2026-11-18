package com.instakill.infrastructure.llm;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Talks to an OpenAI-compatible /v1/chat/completions endpoint (e.g. LM Studio). */
@Component
public class LocalLlmClient {

    private static final String SYSTEM_PROMPT = """
            You rewrite social media captions to be funnier. Keep the original meaning and language, \
            keep it short (under 1000 characters) and reply with ONLY the rewritten text, \
            without quotes or explanations.""";

    private final LocalLlmProperties properties;
    private final RestClient restClient;

    public LocalLlmClient(LocalLlmProperties properties) {
        this.properties = properties;
        Duration timeout = Duration.ofSeconds(properties.timeoutSeconds() > 0 ? properties.timeoutSeconds() : 30);
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(timeout).build());
        factory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public String makeFunnier(String text) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (properties.model() != null && !properties.model().isBlank()) {
            body.put("model", properties.model());
        }
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.add(Map.of("role", "user", "content", text));
        body.put("messages", messages);
        body.put("temperature", 0.8);
        body.put("stream", false);

        JsonNode response;
        try {
            response = restClient.post()
                    .uri(properties.url())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (ResourceAccessException ex) {
            boolean timeout = ex.getCause() instanceof HttpTimeoutException
                    || ex.getCause() instanceof java.net.SocketTimeoutException;
            throw new LlmUnavailableException(
                    timeout ? "The AI server took too long to respond" : "The AI server is not reachable",
                    timeout, ex);
        } catch (RestClientResponseException ex) {
            throw new LlmException("The AI server returned an error (" + ex.getStatusCode().value() + ")", ex);
        } catch (RuntimeException ex) {
            throw new LlmException("The AI server returned an invalid response", ex);
        }

        String content = response == null ? null : response.path("choices").path(0).path("message").path("content").asText(null);
        if (content == null || content.isBlank()) {
            throw new LlmException("The AI server returned an empty response");
        }
        return content.trim();
    }
}
