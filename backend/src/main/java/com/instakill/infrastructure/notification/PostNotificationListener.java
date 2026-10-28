package com.instakill.infrastructure.notification;

import com.instakill.post.domain.PostCreatedEvent;
import com.instakill.user.application.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class PostNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(PostNotificationListener.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final NtfyProperties properties;
    private final UserService userService;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    public PostNotificationListener(NtfyProperties properties, UserService userService) {
        this.properties = properties;
        this.userService = userService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostCreated(PostCreatedEvent event) {
        if (!properties.enabled()) {
            return;
        }
        try {
            String username = userService.getById(event.authorId()).username();
            String message = "Neuer Post von @%s (ID: %s) um %s".formatted(username, event.postId(), event.createdAt());
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.baseUrl() + "/" + properties.topic()))
                    .timeout(TIMEOUT)
                    .header("Title", "Instakill: neuer Post")
                    .POST(HttpRequest.BodyPublishers.ofString(message))
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 300) {
                log.warn("ntfy returned status {}", response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("Could not send post notification: {}", e.toString());
        }
    }
}
