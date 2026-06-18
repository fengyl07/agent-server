// 导入core-js和raf
import '@uyun/react-polyfill'
import App from './app'
import React from 'react'
import ReactDOM from 'react-dom'
import '@uyun/components/dist/index.css'

function render (APP) {
  ReactDOM.render(<APP />, document.getElementById('app'))
}

render(App)

if (module.hot) {
  module.hot.accept('./app.js', () => render(App))
}
