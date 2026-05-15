import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('../views/Dashboard.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/transcription/upload',
    name: 'TranscriptionUpload',
    component: () => import('../views/transcription/Upload.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/transcription/records',
    name: 'TranscriptionRecords',
    component: () => import('../views/transcription/Records.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/transcription/auto-recording',
    name: 'AutoRecording',
    component: () => import('../views/transcription/AutoRecording.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/transcription/vosk-test',
    name: 'VoskTest',
    component: () => import('../views/transcription/VoskTest.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/ai-chat',
    name: 'AiChat',
    component: () => import('../views/AiChat.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/sql-query',
    name: 'SqlQuery',
    component: () => import('../views/SqlQuery.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/files',
    name: 'AdminFiles',
    component: () => import('../views/AdminFiles.vue'),
    meta: { requiresAuth: true, requiresAdmin: true } // 仅限管理员访问
  },
  {
    path: '/ai-question/records',
    name: 'AiQuestionRecords',
    component: () => import('../views/ai-question/Records.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/ai-question/generate',
    name: 'AiQuestionGenerate',
    component: () => import('../views/AiQuestionGenerator.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/class-management/my-classes',
    name: 'MyClasses',
    component: () => import('../views/class-management/MyClasses.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/class-management/class-detail/:classid',
    name: 'ClassDetail',
    component: () => import('../views/class-management/ClassDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/class-management/assign-question/:classid',
    name: 'AssignQuestion',
    component: () => import('../views/class-management/AssignQuestion.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/class-management/create',
    name: 'CreateClass',
    component: () => import('../views/class-management/CreateClass.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/class-management/members',
    name: 'ClassMembers',
    component: () => import('../views/class-management/ClassMembers.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/question-list',
    name: 'QuestionList',
    component: () => import('../views/QuestionList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/answer-question',
    name: 'AnswerQuestion',
    component: () => import('../views/AnswerQuestion.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    // 需要登录但未登录，跳转到登录页
    return next('/login')
  }

  if (to.path === '/login' && userStore.isLoggedIn) {
    // 已登录却访问登录页，重定向到首页
    return next('/')
  }

  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    // 需要管理员权限但不是管理员
    alert('抱歉，您没有权限访问此页面。只有管理员可以访问 SQL 查询功能。')
    return next('/')
  }

  next()
})

export default router
