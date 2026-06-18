import moment from 'moment'
import { __ } from '@uyun/utils'
import React, { Component } from 'react'
import { observer, inject } from 'mobx-react'
import { Button, Form, Input } from '@uyun/components'
import TaskDistribution from './taskModal'
import PageTable from './PageTable'
import './index.less'

const Search = Input.Search

@Form.create()
@inject('todoStore')
@observer
export default class TaskManagement extends Component {
  state = {
    visible: false
  };

  store = this.props.todoStore;

  componentDidMount () {
    this.store.getTodoList()
  }

  taskDistribute = (text, record) => {
    this.store.paramsStore(text, record)
    this.setState({
      visible: true
    })
  };

  handleCancel = () => {
    this.setState({
      visible: false
    })
    this.reset()
  };

  handleOk = e => {
    const { params } = this.store
    e.preventDefault()

    this.props.form.validateFields(async (err, values) => {
      if (!err) {
        await this.store.updateTodo({
          ...params,
          dealsUser: values.dealsUser[0].name
        })
        this.setState({
          visible: false
        })
        this.reset()
      }
    })
  };

  handleSearchChange = type => e => {
    this.store.changeSearch(type, e.target.value)
  };

  handleSearchInput = e => {
    this.store.getInquire()
  };

  taskDelete = record => {
    this.store.removeTodo(record.id)
  };

  reset = () => {
    this.props.form.resetFields()
  };

  render () {
    const { store } = this
    const columns = [
      {
        key: 'content',
        dataIndex: 'content',
        title: __('task_description'),
        width: '20%'
      },
      {
        key: 'hasCompleted',
        dataIndex: 'hasCompleted',
        title: __('task_status'),
        width: '10%',
        render: hasCompleted => {
          if (hasCompleted === false) {
            return '待处理'
          } else {
            return '已完成'
          }
        }
      },
      {
        key: 'createUser',
        dataIndex: 'createUser',
        title: __('task_creator'),
        width: '10%'
      },
      {
        key: 'dealsUser',
        dataIndex: 'dealsUser',
        title: __('task_deal'),
        width: '10%'
      },
      {
        key: 'createTime',
        dataIndex: 'createTime',
        title: __('task_created_time'),
        width: '10%',
        render: createTime => {
          return moment(createTime).format('YYYY-MM-DD')
        }
      },
      {
        key: 'dealsTime',
        dataIndex: 'dealsTime',
        title: __('task_deal_time'),
        width: '10%',
        render: dealsTime => {
          return moment(dealsTime).format('YYYY-MM-DD')
        }
      },
      {
        title: __('task_operation'),
        dataIndex: 'operation',
        key: 'operation',
        width: '30%',
        render: (text, record) => {
          return (
            <div>
              {!record.hasCompleted && this.store.is ? (
                <Button onClick={() => this.taskDistribute(text, record)}>
                  {__('task-distribution')}
                </Button>
              ) : null}
              <div
                className={
                  !record.hasCompleted && this.store.is
                    ? 'operation-diviver'
                    : ''
                }
              />
              <Button
                onClick={() => {
                  this.taskDelete(record)
                }}
              >
                {__('task_delete')}
              </Button>
            </div>
          )
        }
      }
    ]
    return (
      <div style={{ margin: 24 }}>
        <Form layout="inline">
          <Form.Item>
            <Search
              type="primary"
              placeholder={__('task-description')}
              onChange={this.handleSearchChange('content')}
              onPressEnter={this.handleSearchInput}
            />
          </Form.Item>

          <PageTable
            rowKey="content"
            columns={columns}
            store={store}
            onChange={this.handleTableChange}
          />
        </Form>
        <TaskDistribution
          visible={this.state.visible}
          handleCancel={this.handleCancel}
          handleOk={this.handleOk}
          form={this.props.form}
        />
      </div>
    )
  }
}
