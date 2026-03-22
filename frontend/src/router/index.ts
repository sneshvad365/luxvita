import { route } from 'quasar/wrappers'
import { createRouter, createMemoryHistory, createWebHistory, createWebHashHistory } from 'vue-router'
import routes from './routes'
import { LocalStorage } from 'quasar'
import { useProfileStore } from 'src/stores/profile'

export default route(function () {
  const createHistory = process.env.SERVER
    ? createMemoryHistory
    : (process.env.VUE_ROUTER_MODE === 'history' ? createWebHistory : createWebHashHistory)

  const router = createRouter({
    scrollBehavior: () => ({ left: 0, top: 0 }),
    routes,
    history: createHistory(process.env.VUE_ROUTER_BASE),
  })

  router.beforeEach(async (to) => {
    const isLoggedIn = !!LocalStorage.getItem('jwt')
    if (to.meta.requiresAuth && !isLoggedIn) return '/login'
    if ((to.path === '/login' || to.path === '/register') && isLoggedIn) return '/today'

    // Block access to protected pages until profile setup is complete
    if (isLoggedIn && to.meta.requiresAuth && to.path !== '/profile') {
      const profileStore = useProfileStore()
      if (!profileStore.profile && !profileStore.loading) {
        await profileStore.fetch()
      }
      if (!profileStore.setupComplete) {
        profileStore.navigationBlocked = true
        return '/profile'
      }
    }
  })

  return router
})
