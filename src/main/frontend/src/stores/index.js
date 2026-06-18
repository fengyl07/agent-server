import camelCase from 'lodash/camelCase'

const context = require.context('./', false, /\.js$/)
const keys = context.keys().filter(path => path !== './index.js')

const Stores = {}
const stores = {}

keys.forEach(path => {
  const Store = context(path).default
  const name = path.match(/\.\/(\w+)\.js/)[1]

  Stores[`${name}Store`] = Store
  stores[`${camelCase(name)}Store`] = new Store()
})

export { Stores, stores }
