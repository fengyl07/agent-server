import { observer } from 'mobx-react'
import React, { Component } from 'react'
import { Table } from '@uyun/components'
import { oneOfType, string, bool, object, array, func } from 'prop-types'

@observer
class PageTable extends Component {
  static defaultProps = {
    rowKey: 'name'
  }

  static propTypes = {
    rowKey: string,
    store: object.isRequired,
    pagination: oneOfType([object, bool]),
    columns: array.isRequired,
    onChange: func
  }

  render () {
    const {
      rowKey,
      store,
      columns,
      onChange
    } = this.props
    const pagination = 'pagination' in this.props ? this.props.pagination : {
      defaultPageSize: 10,
      showQuickJumper: true,
      showSizeChanger: true,
      current: store.pageIndex,
      pageSize: store.pageSize,
      pageSizeOptions: ['10', '20', '50', '100', '200'],
      total: store.totalCount
    }

    return (
      <Table
        rowKey={rowKey}
        dataSource={store.todoList.toJS() || store.todoList.toJS()}
        loading={store.loading}
        columns={columns}
        onChange={onChange}
        style={{ marginTop: 10 }}
        pagination={pagination}
        size="small"
      />
    )
  }
}
export default PageTable
