import { defineStore } from 'pinia'
import api from 'src/api/client'
import type { Meal, Macros, Insight, ActivityLogResponse } from 'src/api/client'

interface WaterEntry {
  id:       string
  loggedAt: string
  amountL:  number
}

interface TodayState {
  selectedDate:   string
  meals:          Meal[]
  totals:         Macros
  activities:     ActivityLogResponse[]
  waterTotalL:    number
  waterEntries:   WaterEntry[]
  insight:        Insight | null
  loading:        boolean
  insightLoading: boolean
}

const emptyMacros = (): Macros => ({ kcal: 0, proteinG: 0, carbsG: 0, fatG: 0, fiberG: 0 })

export const useTodayStore = defineStore('today', {
  state: (): TodayState => ({
    selectedDate:   new Date().toISOString().slice(0, 10),
    meals:          [],
    totals:         emptyMacros(),
    activities:     [],
    waterTotalL:    0,
    waterEntries:   [],
    insight:        null,
    loading:        false,
    insightLoading: false,
  }),

  actions: {
    async fetchToday(date?: string) {
      this.loading = true
      const d = date ?? this.selectedDate
      try {
        const [mealsRes, actRes, waterRes] = await Promise.all([
          api.get('/api/meals/today',    { params: { date: d } }),
          api.get('/api/activity/today', { params: { date: d } }),
          api.get('/api/water/today',    { params: { date: d } }),
        ])
        this.meals         = mealsRes.data.meals
        this.totals        = mealsRes.data.totals
        this.activities    = actRes.data.activities
        this.waterTotalL   = waterRes.data.totalL
        this.waterEntries  = waterRes.data.entries
      } finally {
        this.loading = false
      }
    },

    async fetchInsight() {
      this.insightLoading = true
      try {
        const { data } = await api.get<Insight>('/api/insights/daily', { params: { date: this.selectedDate } })
        this.insight = data.insight ? data : null
      } catch {
        // insight failure is non-critical
      } finally {
        this.insightLoading = false
      }
    },

    goToDate(date: string) {
      this.selectedDate = date
      this.insight = null
      void this.fetchToday(date)
    },
  },
})
