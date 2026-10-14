package com.instakill.feed.application;

import com.instakill.feed.domain.FeedRankingStrategy;
import com.instakill.feed.domain.FeedSort;
import com.instakill.post.domain.Post;
import com.instakill.post.domain.PostRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedServiceTest {

    private final PostRepository repository = Mockito.mock(PostRepository.class);

    @Test
    void delegatesToStrategyMatchingRequestedSort() {
        Post newPost = post();
        Post trendingPost = post();
        FeedService service = new FeedService(
                List.of(stub(FeedSort.NEW, newPost), stub(FeedSort.TRENDING, trendingPost)), repository);

        assertThat(service.listFeed(FeedSort.NEW, 0, 5)).containsExactly(newPost);
        assertThat(service.listFeed(FeedSort.TRENDING, 0, 5)).containsExactly(trendingPost);
    }

    @Test
    void defaultsToNewWhenSortIsNull() {
        Post newPost = post();
        FeedService service = new FeedService(
                List.of(stub(FeedSort.NEW, newPost), stub(FeedSort.TRENDING, post())), repository);

        assertThat(service.listFeed(null, 0, 5)).containsExactly(newPost);
    }

    @Test
    void passesRepositoryAndPagingToStrategy() {
        FeedRankingStrategy strategy = new FeedRankingStrategy() {
            @Override
            public FeedSort sort() {
                return FeedSort.NEW;
            }

            @Override
            public List<Post> fetch(PostRepository repo, int page, int size) {
                assertThat(repo).isSameAs(repository);
                assertThat(page).isEqualTo(3);
                assertThat(size).isEqualTo(7);
                return List.of();
            }
        };

        new FeedService(List.of(strategy), repository).listFeed(FeedSort.NEW, 3, 7);
    }

    @Test
    void failsWhenNoStrategyIsRegisteredForSort() {
        FeedService service = new FeedService(List.of(stub(FeedSort.NEW, post())), repository);

        assertThatThrownBy(() -> service.listFeed(FeedSort.TRENDING, 0, 5))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void failsWhenTwoStrategiesClaimSameSort() {
        assertThatThrownBy(() -> new FeedService(
                List.of(stub(FeedSort.NEW, post()), stub(FeedSort.NEW, post())), repository))
                .isInstanceOf(IllegalStateException.class);
    }

    private FeedRankingStrategy stub(FeedSort sort, Post result) {
        return new FeedRankingStrategy() {
            @Override
            public FeedSort sort() {
                return sort;
            }

            @Override
            public List<Post> fetch(PostRepository repo, int page, int size) {
                return List.of(result);
            }
        };
    }

    private Post post() {
        Instant now = Instant.now();
        return new Post(UUID.randomUUID(), UUID.randomUUID(), "content", null, now, now, 0, 0);
    }
}
