module.exports = {
  pages: {
    index: {
      entry: './src/index.js',
      template: './public/index.html'
    }
  },
  themes: ['white', 'blue'],
  alias: {
    '@': './src'
  },
  proxy: [
    {
      context: [
        '/api/**',
        '/tenant/**',
        '/notify/**',
        '/portal/**',
        '/userrole/**',
        '/frontend/**'
      ],
      target: 'http://120.26.184.72:7508'
    },
    {
      context: ['/alertagent/frontapi/v1'],
      target: 'http://127.0.0.1:8090'
    }
  ]
}
