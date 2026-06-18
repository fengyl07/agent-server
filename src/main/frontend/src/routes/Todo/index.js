import React from 'react'
import { Row, Col } from '@uyun/components'
import DutyList from './TodoList'
import RotaTodo from './RotaTodo'
import TodoCalendar from './TodoCalendar'
import '@uyun/everest-styles/atomic.less'
import './index.less'

export default function Container () {
  return (
    <div className="p20">
      <Row gutter={16}>
        <Col span={12}>
          <TodoCalendar />
        </Col>
        <Col span={12}>
          <DutyList />
          <RotaTodo />
        </Col>
      </Row>
    </div>
  )
}
