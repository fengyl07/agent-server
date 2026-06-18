# uyun-web-template

基于 react、react-router-dom、mobx、@uyun/components 搭建的基础脚手架。

## 命令

```bash
# 本地开发
$ yarn start

# 本地构建
$ yarn build

# 查看webpack配置
$ yarn inspect

# eslint检测代码风格，并自动修复
$ yarn lint

# 启动本地服务预览打包后的文件
$ yarn serve

# 运行测试用例
$ yarn test
```

## 目录结构

```
.
├── public # 放静态资源
├── src # 源码
│   ├── __mocks__ # mock相关内容
│   ├── __tests__ # 测试相关
│   ├── common # 公共文件
│   ├── components # 项目组件
│   ├── services # api
│   ├── stores # 全局store
│   ├── routes # 对应路由下的师徒文件
│   └── utils # 项目utils
│   ├── index.js # 项目入口
│   ├── app.js # app文件
│   ├── router.js # 定义路由结构
├── .browserslistrc
├── .editorconfig
├── .eslintignore
├── .eslintrc.js
├── .gitignore
├── babel.config.js
├── everest.config.js
├── jest.config.js # 测试配置
├── package.json
├── postcss.config.js
└── README.md
```

## 功能

 模板包含基础的数据流、换肤、国际化等方案。

- [数据流设计](http://view.uyundev.cn/docs/scaffold/mobx-pratice-cn)
- [换肤方案](http://npm.uyundev.cn/package/@uyun/everest-styles)
- [国际化](http://npm.uyundev.cn/package/@uyun/everest-i18n)
