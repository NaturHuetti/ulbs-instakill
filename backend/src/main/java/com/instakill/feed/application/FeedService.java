package com.instakill.feed.application;

import com.instakill.feed.domain.FeedRankingStrategy;
import com.instakill.feed.domain.FeedSort;
import com.instakill.post.domain.Post;
import com.instakill.post.domain.PostRepository;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class FeedService {

    private final Map<FeedSort, FeedRankingStrategy> strategies = new EnumMap<>(FeedSort.class);
    private final PostRepository posts;

    public FeedService(List<FeedRankingStrategy> strategies, PostRepository posts) {
        this.posts = posts;
        for (FeedRankingStrategy strategy : strategies) {
            FeedRankingStrategy previous = this.strategies.put(strategy.sort(), strategy);
            if (previous != null) {
                throw new IllegalStateException("Multiple feed ranking strategies registered for " + strategy.sort());
            }
        }
    }

    public List<Post> listFeed(FeedSort sort, int page, int size) {
        FeedSort effectiveSort = sort == null ? FeedSort.NEW : sort;
        FeedRankingStrategy strategy = strategies.get(effectiveSort);
        if (strategy == null) {
            throw new IllegalStateException("No feed ranking strategy registered for " + effectiveSort);
        }
        return strategy.fetch(posts, page, size);
    }
}
