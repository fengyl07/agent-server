import HttpApi from './swagger-api/index'
import ApiClient from './swagger-api/ApiClient'

const http = new ApiClient()
http.basePath = '/'

module.exports = Object.keys(HttpApi).reduce((items, key) => {
  if (key.slice(3).indexOf('Api') > -1) {
    const api = new HttpApi[key]()
    for (const key in api) {
      items[key] = (...args) =>
        new Promise((resolve, reject) => {
          const callback = (error, data, response) => {
            if (error) return reject(error)
            return resolve(response)
          }
          api[key](...args, callback)
        })
    }
    return items
  }
  return items
}, {})
