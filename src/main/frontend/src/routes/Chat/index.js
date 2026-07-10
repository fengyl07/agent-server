import React, { Component } from 'react'
import { __ } from '@uyun/utils'
import { observer, inject } from 'mobx-react'
import { Button, Input, Icon } from '@uyun/components'
import './index.less'

const { TextArea } = Input

@inject('chatStore')
@observer
export default class Chat extends Component {
  store = this.props.chatStore;

  componentDidMount () {
    this.scrollToBottom()
  }

  componentDidUpdate () {
    this.scrollToBottom()
  }

  scrollToBottom () {
    if (this.listEl) {
      this.listEl.scrollTop = this.listEl.scrollHeight
    }
  }

  handleInputChange = e => {
    this.store.changeInput(e.target.value)
  };

  // Enter 发送，Shift+Enter 换行
  handleKeyDown = e => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      this.store.send()
    }
  };

  handleSend = () => {
    this.store.send()
  };

  handleQuick = text => () => {
    this.store.changeInput(text)
    this.store.send()
  };

  render () {
    const { messages, input, loading } = this.store
    const quickAsks = [
      __('chat-quick-count'),
      __('chat-quick-list'),
      __('chat-quick-sop')
    ]

    return (
      <div className="alert-chat">
        <div className="alert-chat__header">
          <span className="alert-chat__title">
            <Icon type="message" /> {__('chat-title')}
          </span>
          <Button size="small" onClick={() => this.store.reset()}>
            {__('chat-clear')}
          </Button>
        </div>

        <div
          className="alert-chat__body"
          ref={el => {
            this.listEl = el
          }}
        >
          {messages.length === 0 && (
            <div className="alert-chat__empty">
              <p>{__('chat-empty')}</p>
              <div className="alert-chat__quick">
                {quickAsks.map((q, i) => (
                  <Button key={i} size="small" onClick={this.handleQuick(q)}>
                    {q}
                  </Button>
                ))}
              </div>
            </div>
          )}

          {messages.map((msg, index) => (
            <div
              key={index}
              className={`alert-chat__row alert-chat__row--${msg.role}`}
            >
              <div
                className={
                  `alert-chat__bubble alert-chat__bubble--${msg.role}` +
                  (msg.error ? ' is-error' : '')
                }
              >
                <span className="alert-chat__text">{msg.content}</span>
                {msg.role === 'assistant' && msg.intent && (
                  <span className="alert-chat__intent">{msg.intent}</span>
                )}
              </div>
            </div>
          ))}

          {loading && (
            <div className="alert-chat__row alert-chat__row--assistant">
              <div className="alert-chat__bubble alert-chat__bubble--assistant">
                <span className="alert-chat__dots">
                  <i />
                  <i />
                  <i />
                </span>
              </div>
            </div>
          )}
        </div>

        <div className="alert-chat__footer">
          <TextArea
            value={input}
            onChange={this.handleInputChange}
            onKeyDown={this.handleKeyDown}
            placeholder={__('chat-placeholder')}
            autosize={{ minRows: 1, maxRows: 4 }}
            disabled={loading}
          />
          <Button
            type="primary"
            className="alert-chat__send"
            onClick={this.handleSend}
            loading={loading}
          >
            {__('chat-send')}
          </Button>
        </div>
      </div>
    )
  }
}
