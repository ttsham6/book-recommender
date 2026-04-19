import { createRouter, createWebHistory } from 'vue-router'
import SearchView from '../views/SearchView.vue'
import RecommendView from '../views/RecommendView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'search',
      component: SearchView
    },
    {
      path: '/recommend/:id',
      name: 'recommend',
      component: RecommendView,
      props: true
    }
  ]
})

export default router
