import Todo from '@/routes/Todo'
import Task from '@/routes/Task'
import Chat from '@/routes/Chat'
import NotFound from '@/routes/NotFound'

export default [
  /**
   * 开发环境跳/login_admin重定向到/
   */
  process.env.NODE_ENV === 'development' && {
    path: '/login_admin',
    redirect: '/'
  },
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    redirect: '/dashboard/todo',
    routes: [
      {
        path: '/dashboard/todo',
        component: Todo
      },
      {
        path: '/dashboard/task',
        component: Task
      },
      {
        path: '/dashboard/chat',
        component: Chat
      }
    ]
  },
  {
    path: '*',
    component: NotFound
  }
].filter(Boolean)
