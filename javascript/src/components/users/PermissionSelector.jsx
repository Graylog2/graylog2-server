import React from 'react';
import Immutable from 'immutable';

import { TabbedArea } from 'react-bootstrap';
import { TabPane } from 'react-bootstrap';

import TableList from '../common/TableList';
import DataTable from '../common/DataTable';

const PermissionSelector = React.createClass({
  render() {
    return (
      <div>
        <TabbedArea defaultActiveKey={1} animation={false}>
          <TabPane eventKey={1} tab="Streams">
            <TableList
              items={this.props.streams}
              filterLabel="Filter Streams"
              filterKeys={['title']}/>
          </TabPane>
          <TabPane eventKey={2} tab="Dashboards">
            <TableList
              items={this.props.dashboards}
              filterLabel="Filter Dashboards"
              filterKeys={['title']}/>
          </TabPane>
        </TabbedArea>
      </div>
    );
  },
});

export default PermissionSelector;
