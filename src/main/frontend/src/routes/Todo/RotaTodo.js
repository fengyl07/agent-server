import React from 'react'
import { __ } from '@uyun/utils'
import { observer, inject } from 'mobx-react'
import { Row, Col, Button, Checkbox, Icon, Input, Card } from '@uyun/components'
import './index.less'

// 代办计划
@inject('todoStore')
@observer
export default class RotaTodo extends React.Component {
  componentDidMount () {
    this.props.todoStore.getTodoList()
  }

  state = {
    expand: false
  }

  show = () => {
    this.setState({
      expand: !this.state.expand
    })
  }

  onChange = e => {
    this.setState({
      newTodo: e.target.value
    })
  }

  addTodo = () => {
    this.props.todoStore.addTodo(this.state.newTodo)
    this.setState({ newTodo: '' })
  }

  removeTodo = id => {
    this.props.todoStore.removeTodo(id)
  }

  updateTodo = (id, checked) => {
    const params = this.props.todoStore.todoList.toJS()
    const result = params.find(item => item.id === id)
    this.props.todoStore.updateTodo({
      ...result,
      hasCompleted: checked,
      id: `${id}`
    })
  }

  render () {
    return (
      <div className="rotaCalendar">
        <Card title={__('rota-todo-plan')} extra={<Button className="add_btn" onClick={this.show}>{__('rota-add-task')}</Button>}>
          { this.state.expand && <div className="addTodo">
            <Row>
              <Col span={20}>
                <Input onChange={this.onChange} value={this.state.newTodo} placeholder={__('rota-todo-input')} />
              </Col>
              <Col span={4}>
                <Button type="primary" onClick={this.addTodo}>{__('rota-completed')}</Button>
              </Col>
            </Row>
          </div>
          }
          <div className="todo-list-container">
            <ul className="todo-list">
              {this.props.todoStore.todoList.map(todo =>
                (
                  <li className="todo-list-li" key={todo.id} >
                    <Row >
                      <Col span={1}>
                        <Checkbox
                          onChange={() => this.updateTodo(todo.id, todo.hasCompleted)}
                        />
                      </Col>
                      <Col span={22}>{todo.content}</Col>
                      <Col span={1}><Icon onClick={() => this.removeTodo(todo.id)} className="remove-todo" type="close-circle" /></Col>
                    </Row>
                  </li>
                )
              )}
            </ul>
          </div>
        </Card>
      </div>
    )
  }
}
