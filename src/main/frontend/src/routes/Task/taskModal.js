import { __ } from '@uyun/utils'
import React, { Component } from 'react'
import { observer, inject } from 'mobx-react'
import UserPicker from '@uyun/ec-user-picker'
import { Form, Modal } from '@uyun/components'

const FormItem = Form.Item

@Form.create()
@inject('todoStore')
@observer
export default class TaskDistribution extends Component {
  render () {
    const { visible, form, handleCancel, handleOk } = this.props
    const { getFieldDecorator } = form
    const formItemLayout = {
      labelCol: {
        xs: { span: 24 },
        sm: { span: 6 }
      },
      wrapperCol: {
        xs: { span: 24 },
        sm: { span: 14 }
      }
    }
    return (
      <Modal
        visible={visible}
        title={__('task-distribution-title')}
        onCancel={handleCancel}
        onOk={handleOk}
      >
        <Form >
          <FormItem
            {...formItemLayout}
            label={__('task-deal')}
          >
            {getFieldDecorator('dealsUser', {
              initialValue: []
            })(
              <UserPicker
                productNum="APP"
                mode="single"
              />
            )}
          </FormItem>
        </Form>
      </Modal>
    )
  }
}
