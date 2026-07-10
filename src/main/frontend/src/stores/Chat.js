import request from 'superagent'
import { observable, action, runInAction } from 'mobx'

// 后端对话接口：AgentChatController -> /{appcode}/frontapi/v1/agent/chat
// 开发环境由 everest.config.js 代理到后端；生产由平台 Nginx 转发到 -api upstream
const CHAT_API = '/alertagent/frontapi/v1/agent/chat'

function newSessionId () {
  return `web-${Date.now()}`
}

export default class Chat {
  /** 消息列表：{ role: 'user' | 'assistant', content, intent, error, time } */
  @observable messages = [];

  /** 输入框内容 */
  @observable input = '';

  /** 是否等待后端回复 */
  @observable loading = false;

  /** 会话 ID，仅透传回显（后端暂未做多轮记忆） */
  @observable sessionId = newSessionId();

  @action
  changeInput (value) {
    this.input = value
  }

  @action
  reset () {
    this.messages = []
    this.input = ''
    this.loading = false
    this.sessionId = newSessionId()
  }

  @action
  async send () {
    const text = (this.input || '').trim()
    if (!text || this.loading) {
      return
    }

    this.messages.push({ role: 'user', content: text, time: Date.now() })
    this.input = ''
    this.loading = true

    try {
      const res = await request
        .post(CHAT_API)
        .set('Content-Type', 'application/json')
        .send({ message: text, sessionId: this.sessionId })

      const data = (res && res.body) || {}
      runInAction(() => {
        this.loading = false
        this.messages.push({
          role: 'assistant',
          content: data.reply || '未能生成回复，请换个问法试试。',
          intent: data.intent,
          time: Date.now()
        })
      })
    } catch (e) {
      runInAction(() => {
        this.loading = false
        this.messages.push({
          role: 'assistant',
          content: `请求失败：${(e && e.message) || '网络异常，请稍后再试'}`,
          error: true,
          time: Date.now()
        })
      })
    }
  }
}
