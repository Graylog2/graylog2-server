import PropTypes from 'prop-types';
import React from 'react';
import Immutable from 'immutable';


import { Button, ButtonGroup, ButtonToolbar, Tab, Tabs } from 'components/graylog';
import { TableList } from 'components/common';

class PermissionSelector extends React.Component {
  static propTypes = {
    onChange: PropTypes.func,
    streams: PropTypes.object,
    dashboards: PropTypes.object,
    permissions: PropTypes.object,
  };

  render() {
    const streamItemButtons = (stream) => {
      const isRead = this.props.permissions.contains(`streams:read:${stream.id}`);
      const isEdit = this.props.permissions.contains(`streams:edit:${stream.id}`);
      return (
        <ButtonGroup bsSize="small">
          <Button bsStyle={isRead ? 'info' : 'default'}
                  onClick={() => this._toggleStreamReadPermissions(stream)}
                  active={isRead}>Allow reading
          </Button>
          <Button bsStyle={isEdit ? 'info' : 'default'}
                  onClick={() => this._toggleStreamEditPermissions(stream)}
                  active={isEdit}>Allow editing
          </Button>
        </ButtonGroup>
      );
    };

    const multiStreamButtons = (streamIds) => {
      const selectedStreams = this.props.streams.filter((stream) => streamIds.contains(stream.id));
      const allRead = selectedStreams.every((stream) => this.props.permissions.contains(`streams:read:${stream.id}`));
      const allEdit = selectedStreams.every((stream) => this.props.permissions.contains(`streams:edit:${stream.id}`));
      const readActionLabel = allRead ? 'Clear' : 'Set';
      const editActionLabel = allEdit ? 'Clear' : 'Set';

      return (
        <ButtonToolbar>
          <Button bsSize="xsmall" bsStyle="info" onClick={() => this._toggleAllStreamsRead(streamIds, allRead)}>{readActionLabel} read permissions</Button>
          <Button bsSize="xsmall" bsStyle="info" onClick={() => this._toggleAllStreamsEdit(streamIds, allEdit)}>{editActionLabel} edit permissions</Button>
        </ButtonToolbar>
      );
    };

    const dashboardItemButtons = (dashboard) => {
      const isRead = this.props.permissions.contains(`dashboards:read:${dashboard.id}`);
      const isEdit = this.props.permissions.contains(`dashboards:edit:${dashboard.id}`);
      return (
        <ButtonGroup bsSize="small">
          <Button bsStyle={isRead ? 'info' : 'default'}
                  onClick={() => this._toggleDashboardReadPermissions(dashboard)}
                  active={isRead}>Allow reading
          </Button>
          <Button bsStyle={isEdit ? 'info' : 'default'}
                  onClick={() => this._toggleDashboardEditPermissions(dashboard)}
                  active={isEdit}>Allow editing
          </Button>
        </ButtonGroup>
      );
    };
    const multiDashboardButtons = (dashboardIds) => {
      const selectedDashboards = this.props.dashboards.filter((dashboard) => dashboardIds.contains(dashboard.id));
      const allRead = selectedDashboards.every((dashboard) => this.props.permissions.contains(`dashboards:read:${dashboard.id}`));
      const allEdit = selectedDashboards.every((dashboard) => this.props.permissions.contains(`dashboards:edit:${dashboard.id}`));
      const readActionLabel = allRead ? 'Clear' : 'Set';
      const editActionLabel = allEdit ? 'Clear' : 'Set';

      return (
        <div className="pull-right" style={{ marginTop: 10, marginBottom: 10 }}>
          <Button bsSize="xsmall" bsStyle="info" onClick={() => this._toggleAllDashboardsRead(dashboardIds, allRead)}>{readActionLabel} read permissions</Button>
          &nbsp;
          <Button bsSize="xsmall" bsStyle="info" onClick={() => this._toggleAllDashboardsEdit(dashboardIds, allEdit)}>{editActionLabel} edit permissions</Button>
        </div>
      );
    };

    return (
      <div>
        <Tabs id="permissionSelectorTabs" defaultActiveKey={1} animation={false}>
          <Tab eventKey={1} title="Streams">
            <div style={{ marginTop: 10 }}>
              <TableList items={Immutable.List(this.props.streams)}
                         filterLabel="Filter Streams"
                         filterKeys={['title']}
                         itemActionsFactory={streamItemButtons}
                         bulkActionsFactory={multiStreamButtons} />
            </div>
          </Tab>
          <Tab eventKey={2} title="Dashboards">
            <div style={{ marginTop: 10 }}>
              <TableList items={Immutable.List(this.props.dashboards)}
                         filterLabel="Filter Dashboards"
                         filterKeys={['title']}
                         itemActionsFactory={dashboardItemButtons}
                         bulkActionsFactory={multiDashboardButtons} />
            </div>
          </Tab>
        </Tabs>
      </div>
    );
  }

  /*
   * onClick actions for single edits
   */
  _toggleStreamReadPermissions = (stream) => {
    this._toggleReadPermissions('streams', Immutable.Set.of(stream.id));
  };

  _toggleStreamEditPermissions = (stream) => {
    this._toggleEditPermissions('streams', Immutable.Set.of(stream.id));
  };

  _toggleDashboardReadPermissions = (dashboard) => {
    this._toggleReadPermissions('dashboards', Immutable.Set.of(dashboard.id));
  };

  _toggleDashboardEditPermissions = (dashboard) => {
    this._toggleEditPermissions('dashboards', Immutable.Set.of(dashboard.id));
  };

  /*
   * onClick actions for bulk edits
   */

  _toggleAllStreamsRead = (streamIds, clearPermissions) => {
    this._toggleReadPermissions('streams', streamIds, clearPermissions);
  };

  _toggleAllStreamsEdit = (streamIds, clearPermissions) => {
    this._toggleEditPermissions('streams', streamIds, clearPermissions);
  };

  _toggleAllDashboardsRead = (dashboardIds, clearPermissions) => {
    this._toggleReadPermissions('dashboards', dashboardIds, clearPermissions);
  };

  _toggleAllDashboardsEdit = (dashboardIds, clearPermissions) => {
    this._toggleEditPermissions('dashboards', dashboardIds, clearPermissions);
  };

  _toggleReadPermissions = (target, idList, clearPermissions = true) => {
    let added = Immutable.Set.of();
    let deleted = Immutable.Set.of();

    idList.forEach((id) => {
      const readTarget = `${target}:read:${id}`;
      const editTarget = `${target}:edit:${id}`;

      if (this.props.permissions.contains(readTarget) && clearPermissions) {
        deleted = deleted.add(readTarget).add(editTarget);
      } else {
        added = added.add(readTarget);
      }
    }, this);
    this.props.onChange(added, deleted);
  };

  _toggleEditPermissions = (target, idList, clearPermissions = true) => {
    let added = Immutable.Set.of();
    let deleted = Immutable.Set.of();

    idList.forEach((id) => {
      const readTarget = `${target}:read:${id}`;
      const editTarget = `${target}:edit:${id}`;

      if (this.props.permissions.contains(editTarget) && clearPermissions) {
        deleted = deleted.add(editTarget);
      } else {
        added = added.add(readTarget).add(editTarget);
      }
    }, this);
    this.props.onChange(added, deleted);
  };
}

export default PermissionSelector;
