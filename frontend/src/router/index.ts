import { route } from 'quasar/wrappers'
import { createRouter, createMemoryHistory, createWebHistory, createWebHashHistory } from 'vue-router'
import routes from './routes'
import { LocalStorage } from 'quasar'

export default route(function () {
  const createHistory = process.env.SERVER
    ? createMemoryHistory
    : (process.env.VUE_ROUTER_MODE === 'history' ? createWebHistory : createWebHashHistory)

  const router = createRouter({
    scrollBehavior: () => ({ left: 0, top: 0 }),
    routes,
    history: createHistory(process.env.VUE_ROUTER_BASE),
  })

  router.beforeEach((to) => {
    const isLoggedIn = !!LocalStorage.getItem('jwt')
    if (to.meta.requiresAuth && !isLoggedIn) return '/login'
    if ((to.path === '/login' || to.path === '/register') && isLoggedIn) return '/today'
  })

  return router
})
