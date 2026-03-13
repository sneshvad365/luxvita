import { defineStore } from 'pinia'
import api from 'src/api/client'
import type { MacroEstimate, ActivityLogResponse } from 'src/api/client'
import { useTodayStore } from './today'

export const useLogStore = defineStore('log', {
  state: () => ({
    pendingEstimate: null as MacroEstimate | null,
    submitting:      false,
    error:           null as string | null,
  }),

  actions: {
    async submitMeal(description: string | null, photoBase64: string | null) {
      this.submitting = true
      this.error      = null
      try {
        const payload: Record<string, string> = {}
        if (description) payload.description = description
        if (photoBase64)  payload.photo       = photoBase64

        const { data } = await api.post('/api/meals', payload)
        this.pendingEstimate = data.estimate as MacroEstimate

        // Refresh today totals
        await useTodayStore().fetchToday()
        return data
      } catch (e: unknown) {
        this.error = (e as { response?: { data?: { message?: string } } })
          ?.response?.data?.message ?? 'Failed to log meal'
        throw e
      } finally {
        this.submitting = false
      }
    },

    async submitActivity(entry: string): Promise<ActivityLogResponse> {
      const { data } = await api.post<ActivityLogResponse>('/api/activity', { entry })
      return data
    },

    clearEstimate() {
      this.pendingEstimate = null
    },
  },
})
