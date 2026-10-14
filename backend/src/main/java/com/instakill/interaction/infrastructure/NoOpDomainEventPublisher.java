package com.instakill.interaction.infrastructure;

import com.instakill.interaction.domain.DomainEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Default publisher that discards events. Replace by registering another
 * {@link DomainEventPublisher} (e.g. annotated {@code @Primary}).
 */
@Component
public class NoOpDomainEventPublisher implements DomainEventPublisher {

    @Override
    public void publish(Object event) {
        // intentionally empty
//        System.out.println("EVENT");
    }
}
