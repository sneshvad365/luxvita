import { defineStore } from 'pinia'
import { LocalStorage } from 'quasar'
import api from 'src/api/client'
import type { AuthResponse } from 'src/api/client'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token:  LocalStorage.getItem<string>('jwt')    ?? null,
    userId: LocalStorage.getItem<string>('userId') ?? null,
  }),

  getters: {
    isLoggedIn: (s) => !!s.token,
  },

  actions: {
    async register(email: string, password: string) {
      const { data } = await api.post<AuthResponse>('/api/auth/register', { email, password })
      this._persist(data)
    },

    async login(email: string, password: string) {
      const { data } = await api.post<AuthResponse>('/api/auth/login', { email, password })
      this._persist(data)
    },

    logout() {
      this.token  = null
      this.userId = null
      LocalStorage.remove('jwt')
      LocalStorage.remove('userId')
    },

    _persist(data: AuthResponse) {
      this.token  = data.token
      this.userId = data.userId
      LocalStorage.set('jwt', data.token)
      LocalStorage.set('userId', data.userId)
    },
  },
})
