import React from 'react'
import { __ } from '@uyun/utils'
import { observer, inject } from 'mobx-react'
import { Row, Col, Title, Card, Tag } from '@uyun/components'

// 值班人员
@inject('todoStore')
@observer
class DutyList extends React.Component {
  componentDidMount () {
    this.props.todoStore.getDutyList()
  }

  store = this.props.todoStore

  render () {
    const PersonList = ({ users }) => {
      return (
        <Col>
          {
            users.length > 0
              ? users.map((user, index) => <Tag key={index}>{user}</Tag>)
              : __('rota-nostaff-duty')
          }
        </Col>
      )
    }

    const Dutys = this.store.dutyList && this.store.dutyList.length > 0
      ? this.store.dutyList.map((item, index) => (
        <Col span={12} key={index}>
          <Row>
            <Col>{item.shiftName}</Col>
          </Row>
          <Row>
            <PersonList users={item.users} />
          </Row>
        </Col>

      )) : <Row>
        <Col>{__('rota-notable-duty')}</Col>
      </Row>

    return (
      <div className="dutyList">
        <Title children={__('rota-duty-staff')} />
        <Card>
          <div className="detail">
            <Row>
              {Dutys}
            </Row>
          </div>
        </Card>
      </div>
    )
  }
}

export default DutyList
