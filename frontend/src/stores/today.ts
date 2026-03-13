import { defineStore } from 'pinia'
import api from 'src/api/client'
import type { Meal, Macros, Insight } from 'src/api/client'

interface TodayState {
  meals:   Meal[]
  totals:  Macros
  insight: Insight | null
  loading: boolean
}

const emptyMacros = (): Macros => ({ kcal: 0, proteinG: 0, carbsG: 0, fatG: 0, fiberG: 0 })

export const useTodayStore = defineStore('today', {
  state: (): TodayState => ({
    meals:   [],
    totals:  emptyMacros(),
    insight: null,
    loading: false,
  }),

  actions: {
    async fetchToday() {
      this.loading = true
      try {
        const { data } = await api.get('/api/meals/today')
        this.meals  = data.meals
        this.totals = data.totals
      } finally {
        this.loading = false
      }
    },

    async fetchInsight() {
      try {
        const { data } = await api.get<Insight>('/api/insights/daily')
        this.insight = data
      } catch {
        // insight failure is non-critical
      }
    },
  },
})
