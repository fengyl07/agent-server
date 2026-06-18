import moment from 'moment'
import router from './router'
import { stores } from './stores'
import Menus from './common/menu'
import { Provider } from 'mobx-react'
import cookie from '@uyun/utils/cookie'
import __, { intl } from '@uyun/utils/i18n'
import locales from './common/locales.json'
import React, { PureComponent } from 'react'
import BasicLayout from '@uyun/ec-basic-layout'
import renderRoutes from './utils/renderRoutes'
import { HashRouter, Link } from 'react-router-dom'
import { LocaleProvider, Icon } from '@uyun/components'
import enUS from '@uyun/components/lib/locale-provider/en_US'
import zhCN from '@uyun/components/lib/locale-provider/zh_CN'
import 'moment/locale/zh-cn'

intl.merge(locales)
moment.locale('zh-cn')
moment.defaultFormat = 'YYYY-MM-DD HH:mm'

export default class App extends PureComponent {
  get menus () {
    return [
      {
        key: 'dashboard',
        name: __('menu-dashboard'),
        type: 'group',
        path: 'dashboard',
        children: [
          {
            key: 'todo',
            name: __('menu-todo'),
            type: 'link',
            icon: <Icon type="calendar" />,
            path: 'todo'
          },
          {
            key: 'task',
            name: __('menu-task'),
            type: 'link',
            icon: <Icon type="pushpin" />,
            path: 'task'
          }
        ]
      },
      {
        key: 'authority-management',
        name: __('menu-authority-management'),
        type: 'link',
        icon: <Icon type="setting" />,
        path: `${location.origin}/userrole/`,
        below: true
      }
    ]
  }

  render () {
    const locale = cookie.get('language') === 'en_US' ? enUS : zhCN

    return (
      <Provider {...stores}>
        <LocaleProvider locale={locale}>
          <Menus.Provider value={this.menus}>
            <HashRouter>
              <BasicLayout sideMenu={{ items: this.menus, Link: Link }}>{renderRoutes(router)}</BasicLayout>
            </HashRouter>
          </Menus.Provider>
        </LocaleProvider>
      </Provider>
    )
  }
}
