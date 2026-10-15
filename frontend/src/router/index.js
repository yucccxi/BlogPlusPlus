import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import HomeView from '@/views/HomeView.vue'
import PostView from '@/views/PostView.vue'
import ConsoleView from '@/views/ConsoleView.vue'
import ConsoleOverview from '@/views/ConsoleOverview.vue'
import ConsoleEditor from '@/views/ConsoleEditor.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: MainLayout,
      children: [
        { path: '', name: 'home', component: HomeView },
        { path: 'posts/:slug', name: 'post', component: PostView },
      ],
    },
    {
      path: '/console',
      component: ConsoleView,
      children: [
        { path: '', name: 'console-overview', component: ConsoleOverview },
        { path: 'new', name: 'console-new', component: ConsoleEditor },
        { path: 'edit/:id', name: 'console-edit', component: ConsoleEditor },
      ],
    },
  ],
})

export default router
