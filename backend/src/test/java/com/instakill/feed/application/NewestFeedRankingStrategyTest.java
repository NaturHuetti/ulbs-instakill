package com.instakill.feed.application;

import com.instakill.feed.domain.FeedSort;
import com.instakill.post.domain.Post;
import com.instakill.post.domain.PostRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NewestFeedRankingStrategyTest {

    private final PostRepository repository = Mockito.mock(PostRepository.class);
    private final NewestFeedRankingStrategy strategy = new NewestFeedRankingStrategy();

    @Test
    void handlesNewSort() {
        assertThat(strategy.sort()).isEqualTo(FeedSort.NEW);
    }

    @Test
    void returnsLatestPostsUnchanged() {
        Instant now = Instant.now();
        List<Post> posts = List.of(post(now.minusSeconds(10), 1, 1), post(now.minusSeconds(20), 2, 2));
        when(repository.findLatest(0, 2)).thenReturn(posts);

        List<Post> result = strategy.fetch(repository, 0, 2);

        assertThat(result).isEqualTo(posts);
        verify(repository).findLatest(0, 2);
    }

    private Post post(Instant createdAt, long likes, long comments) {
        return new Post(UUID.randomUUID(), UUID.randomUUID(), "content", null, createdAt, createdAt, likes, comments);
    }
}
