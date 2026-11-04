package com.instakill.analytics.application;

import com.instakill.analytics.application.AnalyticsRepository.MediaTypeAverage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AnalyticsService {

    private static final int TOP_POSTS_LIMIT = 5;

    private final AnalyticsRepository repository;

    public AnalyticsService(AnalyticsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public UserAnalytics analyticsFor(UUID userId) {
        var totals = repository.totals(userId);
        Double ratio = totals.comments() == 0 ? null : (double) totals.likes() / totals.comments();

        var byType = repository.averageLikesByMediaType(userId);
        var media = List.of(find(byType, "photo"), find(byType, "video"));

        var rank = repository.engagementRank(userId)
                .map(r -> new UserAnalytics.Ranking(r.averageEngagement(), r.rank(), r.totalUsers(),
                        r.rank() * 100.0 / r.totalUsers()))
                .orElse(null);

        return new UserAnalytics(
                repository.weeklyStats(userId),
                repository.topPostsByEngagement(userId, TOP_POSTS_LIMIT),
                new UserAnalytics.LikeCommentRatio(totals.likes(), totals.comments(), ratio),
                media,
                rank);
    }

    private static MediaTypeAverage find(List<MediaTypeAverage> list, String type) {
        return list.stream().filter(m -> m.mediaType().equals(type)).findFirst()
                .orElse(new MediaTypeAverage(type, 0, 0));
    }

    public record UserAnalytics(
            List<AnalyticsRepository.WeeklyStats> weekly,
            List<AnalyticsRepository.TopPost> topPosts,
            LikeCommentRatio likeCommentRatio,
            List<MediaTypeAverage> averageLikesByMediaType,
            Ranking ranking) {

        /** ratio is null when there are no comments. */
        public record LikeCommentRatio(long likes, long comments, Double ratio) {
        }

        /** topPercent: 10.0 means the user is within the top 10% of users. Null overall if the user has no posts. */
        public record Ranking(double averageEngagement, long rank, long totalUsers, double topPercent) {
        }
    }
}
