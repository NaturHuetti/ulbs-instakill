<template>
  <section class="analytics">
    <p v-if="error" class="error">{{ error }}</p>
    <p v-else-if="!data">Loading analytics...</p>

    <template v-else>
      <div class="stat-grid">
        <div class="card stat">
          <small>Like-to-comment ratio</small>
          <strong>{{ ratioText }}</strong>
          <small>{{ data.likeCommentRatio.likes }} likes / {{ data.likeCommentRatio.comments }} comments</small>
        </div>
        <div class="card stat">
          <small>Your rank</small>
          <strong v-if="data.ranking">Top {{ formatNumber(data.ranking.topPercent) }}%</strong>
          <strong v-else>–</strong>
          <small v-if="data.ranking">
            #{{ data.ranking.rank }} of {{ data.ranking.totalUsers }} users · avg
            {{ formatNumber(data.ranking.averageEngagement) }} engagement/post
          </small>
          <small v-else>Publish a post to get ranked</small>
        </div>
      </div>

      <div class="card">
        <h3>Per week</h3>
        <p v-if="!data.weekly.length" class="muted">No activity yet.</p>
        <table v-else>
          <thead>
            <tr><th>Week of</th><th>Posts</th><th>Likes</th><th>Comments</th></tr>
          </thead>
          <tbody>
            <tr v-for="w in data.weekly" :key="w.weekStart">
              <td>{{ w.weekStart }}</td><td>{{ w.posts }}</td><td>{{ w.likes }}</td><td>{{ w.comments }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="card">
        <h3>Top 5 posts by engagement</h3>
        <p v-if="!data.topPosts.length" class="muted">No posts yet.</p>
        <ol v-else class="top">
          <li v-for="p in data.topPosts" :key="p.id">
            <span class="title">{{ p.content }}</span>
            <span class="muted">{{ p.likes }} likes + {{ p.comments }} comments =</span>
            <b>{{ p.engagement }}</b>
          </li>
        </ol>
      </div>

      <div class="card">
        <h3>Average likes per post by media type</h3>
        <div class="bars">
          <div v-for="m in data.averageLikesByMediaType" :key="m.mediaType" class="bar-row">
            <span class="label">{{ m.mediaType === 'video' ? 'Videos' : 'Photos' }} ({{ m.posts }})</span>
            <div class="bar"><div class="fill" :style="{ width: barWidth(m.averageLikes) }"></div></div>
            <b>{{ formatNumber(m.averageLikes) }}</b>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { fetchAnalytics } from './api'

const data = ref(null)
const error = ref('')

onMounted(async () => {
  try {
    data.value = await fetchAnalytics()
  } catch (e) {
    error.value = e.response?.data?.detail || 'Failed to load analytics'
  }
})

const ratioText = computed(() => {
  const r = data.value?.likeCommentRatio?.ratio
  return r == null ? 'n/a' : formatNumber(r) + ' : 1'
})

const maxAvg = computed(() => Math.max(...(data.value?.averageLikesByMediaType ?? []).map(m => m.averageLikes), 0))

function barWidth(value) {
  return maxAvg.value === 0 ? '0%' : `${(value / maxAvg.value) * 100}%`
}

function formatNumber(n) {
  return Number(n).toLocaleString(undefined, { maximumFractionDigits: 2 })
}
</script>

<style scoped>
.analytics { display: flex; flex-direction: column; gap: 1.5rem; }
.card { background: #fff; border-radius: 16px; padding: 1.5rem; box-shadow: 0 10px 30px rgba(17, 24, 39, 0.1); }
.card h3 { margin-top: 0; }
.stat-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 1.5rem; }
.stat { display: flex; flex-direction: column; gap: 0.25rem; }
.stat strong { font-size: 2rem; }
.muted { color: #6b7280; }
.error { color: #ef4444; }
table { width: 100%; border-collapse: collapse; }
th, td { text-align: left; padding: 0.4rem 0.5rem; border-bottom: 1px solid #f3f4f6; }
.top { padding-left: 1.2rem; margin: 0; }
.top li { padding: 0.35rem 0; }
.top .title { font-weight: 600; margin-right: 0.5rem; }
.bar-row { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.5rem; }
.bar-row .label { width: 7rem; }
.bar { flex: 1; background: #e5e7eb; border-radius: 999px; height: 0.9rem; overflow: hidden; }
.fill { background: #2563eb; height: 100%; }
</style>
