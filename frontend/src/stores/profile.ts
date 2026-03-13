import { defineStore } from 'pinia'
import api from 'src/api/client'
import type { UserProfile } from 'src/api/client'

export const useProfileStore = defineStore('profile', {
  state: () => ({
    profile: null as UserProfile | null,
    loading: false,
  }),

  actions: {
    async fetch() {
      this.loading = true
      try {
        const { data } = await api.get<UserProfile>('/api/profile')
        this.profile = data
      } catch (e: unknown) {
        if ((e as { response?: { status?: number } })?.response?.status === 404) {
          this.profile = null
        }
      } finally {
        this.loading = false
      }
    },

    async save(updates: Omit<UserProfile, 'id' | 'userId' | 'updatedAt'>) {
      const { data } = await api.put<UserProfile>('/api/profile', updates)
      this.profile = data
    },
  },
})
