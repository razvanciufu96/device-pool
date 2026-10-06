import { createRouter, createWebHistory } from 'vue-router'
import { currentUser } from './session'
import LoginView from './views/LoginView.vue'
import DevicesView from './views/DevicesView.vue'
import MyReservationsView from './views/MyReservationsView.vue'
import DamageView from './views/DamageView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/devices' },
    { path: '/login', component: LoginView, meta: { public: true } },
    { path: '/devices', component: DevicesView },
    { path: '/my', component: MyReservationsView },
    { path: '/damage', component: DamageView },
  ],
})

router.beforeEach((to) => {
  if (!to.meta.public && !currentUser.value) return '/login'
})

export default router
