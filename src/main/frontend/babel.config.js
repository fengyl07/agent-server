module.exports = {
  presets: [
    [
      '@uyun/babel-preset-everest',
      {
        '@babel/preset-env': {
          modules: 'commonjs'
        }
      }
    ]
  ],
  plugins: [
    [
      'import',
      {
        libraryName: '@uyun/components'
      },
      '@uyun/components'
    ],
    [
      '@uyun/everest-i18n/babel',
      {
        locales: './src/common/locales.json',
        translators: ['__']
      }
    ]
  ]
}
