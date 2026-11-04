package com.instakill.analytics.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnalyticsRepository {

    List<WeeklyStats> weeklyStats(UUID userId);

    List<TopPost> topPostsByEngagement(UUID userId, int limit);

    Totals totals(UUID userId);

    List<MediaTypeAverage> averageLikesByMediaType(UUID userId);

    /** Rank of the user's average engagement per post among all users with posts; empty if user has no posts. */
    Optional<UserRank> engagementRank(UUID userId);

    record WeeklyStats(String weekStart, long posts, long likes, long comments) {
    }

    record TopPost(UUID id, String content, String imageUrl, long likes, long comments) {
        @com.fasterxml.jackson.annotation.JsonProperty("engagement")
        public long engagement() {
            return likes + comments;
        }
    }

    record Totals(long posts, long likes, long comments) {
    }

    record MediaTypeAverage(String mediaType, long posts, double averageLikes) {
    }

    record UserRank(double averageEngagement, long rank, long totalUsers) {
    }
}
