import React from 'react'
import { Switch, Route, Redirect } from 'react-router-dom'

export const formatRouter = (routes, parent = '/') => {
  return routes.reduce(
    (router, route) => {
      if (route.component) {
        router.routes.push({
          path: route.path.indexOf('/') === 0 ? route.path : `${parent}/${route.path}`.replace(/\/+/, '/'),
          exact: route.exact,
          strict: route.strict,
          component: route.component
        })
      } else if (route.redirect) {
        router.redirects.push({
          path: route.path,
          exact: true,
          to: route.redirect
        })
      }
      if (route.routes) {
        const { routes, redirects } = formatRouter(route.routes, route.path)
        router.routes.push(...routes)
        router.redirects.push(...redirects)
      }
      return router
    },
    { redirects: [], routes: [] }
  )
}
/**
 * 渲染route数组
 * @param {*} routes
 */
export default routes => {
  const router = formatRouter(routes)

  return (
    <Switch>
      {router.redirects.map((route, i) => {
        return <Redirect key={`redirect-${i}`} {...route} />
      })}
      {router.routes.map(({ path, component, ...route }, i) => {
        return <Route key={`routes-${i}`} path={path} component={component} {...route} />
      })}
    </Switch>
  )
}
