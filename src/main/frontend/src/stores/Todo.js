import moment from 'moment'
import { observable, action, runInAction } from 'mobx'
import {
  deleteTodoById,
  insertTodo,
  queryDutys,
  selectTodoFindAll,
  updateTodoById,
  queryByContent,
  priviRequests
} from '@/utils/swagger-request'

export default class Rota {
  params = {};

  @observable
  loading = false;

  @observable
  search = '';

  @observable
  sorter = {};

  @observable
  list = [];

  @observable
  page = 1;

  @observable
  pageSize = 10;

  @observable
  total = 0;

  /* 定义前端本地数据 */
  @observable curDate = moment().format('YYYY-MM-DD');

  @observable todoList = [];

  @observable dutyList = [];

  @observable content = '';

  @observable dealsUser = '';

  @observable params = {};

  @observable codeResult = [];

  @observable is = false;

  queryString () {
    return Object.keys(this.params)
      .map(key => `${key}=${this.params[key]}`)
      .join('&')
  }

  @action
  changeSearch (value) {
    this.search = value
  }

  @action
  changeTable (page, pageSize, sorter) {
    this.sorter = sorter
    this.page = page
    this.pageSize = pageSize
  }

  @action
  async getList (params = {}, ...args) {
    this.loading = true

    if (this.page) {
      params.page = this.page
    }

    if (this.pageSize) {
      params.pageSize = this.pageSize
    }

    if (this.sorter.columnKey && this.sorter.order) {
      params.sortField = this.sorter.columnKey
      params.order = this.sorter.order === 'descend' ? 1 : 0
    }

    try {
      const data = await this.request(params, ...args)
      runInAction(() => {
        this.loading = false
        this.list = data.body.data
        this.total = parseInt(data.body.total)
      })
    } catch (e) {
      runInAction(() => {
        this.loading = false
      })
    }
  }

  request (date = this.curDate) {
    return selectTodoFindAll(moment(date).valueOf())
  }

  @action
  async privilegesManage (params) {
    const result = await priviRequests(params)
    runInAction(() => {
      const arr = []
      for (const key of result) {
        arr.push(key.code)
      }
      this.is = arr.indexOf('task_assign') > -1
      this.codeResult = arr
    })
  }

  @action
  paramsStore (text, record) {
    runInAction(() => {
      this.params = record
    })
  }

  /** 定义前端操作 */
  @action
  setCurDate (val) {
    this.curDate = val
  }

  @action
  async getDutyList (date = this.curDate) {
    const result = await queryDutys(moment(date).valueOf())
    runInAction(() => {
      this.dutyList = result.body
    })
  }

  @action
  async getTodoList (date = this.curDate) {
    const result = await selectTodoFindAll(moment(date).valueOf())
    runInAction(() => {
      this.todoList = result.body
    })
  }

  @action
  async getInquire (date = this.curDate) {
    const result = await queryByContent(moment(date).valueOf(), this.content)
    runInAction(() => {
      this.todoList = result.body
    })
  }

  @action
  async addTodo (todo) {
    const date = this.curDate ? this.curDate : moment().format('YYYY-MM-DD')
    await insertTodo({
      createTodo: { content: todo, createTime: moment(date).valueOf() }
    })
    runInAction(() => {
      this.getTodoList()
    })
  }

  @action
  async updateTodo (params) {
    await updateTodoById({ updateTodo: params })
    runInAction(() => {
      this.getTodoList()
    })
  }

  @action
  async removeTodo (id) {
    await deleteTodoById(id)
    runInAction(() => {
      this.getTodoList()
    })
  }
}
