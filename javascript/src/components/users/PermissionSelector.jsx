import React from 'react';
import Immutable from 'immutable';

import { TabbedArea, TabPane, Button, ButtonGroup} from 'react-bootstrap';

import TableList from '../common/TableList';
import DataTable from '../common/DataTable';

const PermissionSelector = React.createClass({

  _toggleStreamReadPermissions(stream) {
    this._toggleReadPermissions('streams', stream.id);
  },
  _toggleStreamEditPermissions(stream) {
    this._toggleEditPermissions('streams', stream.id);
  },

  _toggleDashboardReadPermissions(dashboard) {
    this._toggleReadPermissions('dashboards', dashboard.id);
  },
  _toggleDashboardEditPermissions(dashboard) {
    this._toggleEditPermissions('dashboards', dashboard.id);
  },

  _toggleReadPermissions(target, id) {
    const readTarget = target + ':read:' + id;
    const editTarget = target + ':edit:' + id;

    let added = Immutable.Set();
    let deleted = Immutable.Set();
    if (this.props.permissions.contains(readTarget)) {
      deleted = deleted.add(readTarget).add(editTarget);
    } else {
      added = added.add(readTarget);
    }
    this.props.onChange(added, deleted);
  },
  _toggleEditPermissions(target, id) {
    const readTarget = target + ':read:' + id;
    const editTarget = target + ':edit:' + id;

    let added = Immutable.Set();
    let deleted = Immutable.Set();
    if (this.props.permissions.contains(editTarget)) {
      deleted = deleted.add(editTarget);
    } else {
      added = added.add(readTarget).add(editTarget);
    }
    this.props.onChange(added, deleted);
  },

  render() {
    const streamItemButtons = (stream) => {
      const isRead = this.props.permissions.contains(`streams:read:${stream.id}`);
      const isEdit = this.props.permissions.contains(`streams:edit:${stream.id}`);
      return (<ButtonGroup bsSize="small">
        <Button bsStyle={isRead ? 'info' : 'default'} onClick={() => this._toggleStreamReadPermissions(stream)}
                active={isRead}>Allow reading</Button>
        <Button bsStyle={isEdit ? 'info' : 'default'} onClick={() => this._toggleStreamEditPermissions(stream)}
                active={isEdit}>Allow editing</Button>
      </ButtonGroup>);
    };
    const dashboardItemButtons = (dashboard) => {
      const isRead = this.props.permissions.contains(`dashboards:read:${dashboard.id}`);
      const isEdit = this.props.permissions.contains(`dashboards:edit:${dashboard.id}`);
      return (<ButtonGroup bsSize="small">
        <Button bsStyle={isRead ? 'info' : 'default'} onClick={() => this._toggleDashboardReadPermissions(dashboard)}
                active={isRead}>Allow reading</Button>
        <Button bsStyle={isEdit ? 'info' : 'default'} onClick={() => this._toggleDashboardEditPermissions(dashboard)}
                active={isEdit}>Allow editing</Button>
      </ButtonGroup>);
    };
    return (
      <div>
        <TabbedArea defaultActiveKey={1} animation={false}>
          <TabPane eventKey={1} tab="Streams">
            <TableList
              items={this.props.streams}
              filterLabel="Filter Streams"
              filterKeys={['title']}
              itemActionsFactory={streamItemButtons}
            />
          </TabPane>
          <TabPane eventKey={2} tab="Dashboards">
            <TableList
              items={this.props.dashboards}
              filterLabel="Filter Dashboards"
              filterKeys={['title']}
              itemActionsFactory={dashboardItemButtons}
            />
          </TabPane>
        </TabbedArea>
      </div>
    );
  },
});

export default PermissionSelector;
