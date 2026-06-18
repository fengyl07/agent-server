import __ from '@uyun/utils/i18n'
import React, { Component } from 'react'
import { Link } from 'react-router-dom'
import { withRouter } from 'react-router'
import pathToRegexp from 'path-to-regexp'
import { Breadcrumb } from '@uyun/components'
import Menus from '@/common/menu'

@withRouter
export default class PageHeader extends Component {
  getBreadcrumbs = menus => {
    const breadcrumbItems = this.props.location.pathname
      .split('/')
      .filter(i => i)
      .reduce((paths, path, index) => {
        paths.push(`${paths[index - 1] || ''}/${path}`)
        return paths
      }, [])
      .map((url, index) => {
        const breadcrumb = this.getFlatMenus(menus).find(({ path }) => pathToRegexp(path).test(url)) || {}
        if (!breadcrumb.name || breadcrumb.hideInBreadcrumb) return null
        return (
          <Breadcrumb.Item key={url}>
            <Link to={url}>{breadcrumb.name}</Link>
          </Breadcrumb.Item>
        )
      })

    breadcrumbItems.unshift(
      <Breadcrumb.Item key="home">
        <Link to="/">{__('menu-index')}</Link>
      </Breadcrumb.Item>
    )

    return <Breadcrumb separator={'/'}>{breadcrumbItems}</Breadcrumb>
  }

  getFlatMenus (menus, parentPath = '') {
    return menus.reduce((menus, menu) => {
      const path = `${parentPath}/${menu.path}`
      menus.push({
        ...menu,
        path
      })

      if (menu.children) {
        menus.push(...this.getFlatMenus(menu.children, path))
      }
      return menus
    }, [])
  }

  render () {
    return (
      <div className="page-header">
        <Menus.Consumer>{value => this.getBreadcrumbs(value)}</Menus.Consumer>
      </div>
    )
  }
}
