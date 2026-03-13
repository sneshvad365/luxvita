import { defineStore } from 'pinia'
import api from 'src/api/client'
import type { WeightTrendResponse, TrendsSummary, Insight } from 'src/api/client'

export const useTrendsStore = defineStore('trends', {
  state: () => ({
    weightTrend: null as WeightTrendResponse | null,
    summary:     null as TrendsSummary | null,
    insights:    [] as Insight[],
    loading:     false,
  }),

  actions: {
    async fetchAll() {
      this.loading = true
      try {
        const [trendRes, summaryRes] = await Promise.all([
          api.get<WeightTrendResponse>('/api/weight/trend'),
          api.get<TrendsSummary>('/api/trends/summary'),
        ])
        this.weightTrend = trendRes.data
        this.summary     = summaryRes.data
      } finally {
        this.loading = false
      }
    },

    async fetchWeeklyInsights() {
      try {
        const { data } = await api.get<Insight[]>('/api/insights/weekly')
        this.insights = data
      } catch {
        // non-critical
      }
    },

    async logWeight(weightKg: number) {
      await api.post('/api/weight', { weightKg })
      await this.fetchAll()
    },
  },
})
