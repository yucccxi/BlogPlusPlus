import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import HomeView from '@/views/HomeView.vue'
import PostView from '@/views/PostView.vue'
import SearchView from '@/views/SearchView.vue'
import ConsoleView from '@/views/ConsoleView.vue'
import ConsoleOverview from '@/views/ConsoleOverview.vue'
import ConsoleEditor from '@/views/ConsoleEditor.vue'
import ConsoleFiles from '@/views/ConsoleFiles.vue'
import NotFoundView from '@/views/NotFoundView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: MainLayout,
      children: [
        { path: '', name: 'home', component: HomeView },
        { path: 'posts/:slug', name: 'post', component: PostView },
        { path: 'search', name: 'search', component: SearchView },
      ],
    },
    {
      path: '/console',
      component: ConsoleView,
      children: [
        { path: '', name: 'console-overview', component: ConsoleOverview },
        { path: 'new', name: 'console-new', component: ConsoleEditor },
        { path: 'edit/:id', name: 'console-edit', component: ConsoleEditor },
        { path: 'files', name: 'console-files', component: ConsoleFiles },
      ],
    },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundView },
  ],
})

export default router
