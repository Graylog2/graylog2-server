import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import ImmutablePropTypes from 'react-immutable-proptypes';
import { Alert } from 'react-bootstrap';

import Dashboard from './Dashboard';
import EditDashboardModalTrigger from './EditDashboardModalTrigger';
import PermissionsMixin from '../../util/PermissionsMixin';

const DashboardList = createReactClass({
  displayName: 'DashboardList',

  propTypes: {
    dashboards: ImmutablePropTypes.list,
    permissions: PropTypes.arrayOf(PropTypes.string),
  },

  mixins: [PermissionsMixin],

  _formatDashboard(dashboard) {
    return (
      <Dashboard key={`dashboard-${dashboard.id}`} dashboard={dashboard} permissions={this.props.permissions} />
    );
  },

  render() {
    if (this.props.dashboards.isEmpty()) {
      let createDashboardButton;

      if (this.isPermitted(this.props.permissions, ['dashboards:create'])) {
        createDashboardButton = (
          <span>
            <EditDashboardModalTrigger action="create" buttonClass="btn-link btn-text">
              Create one now
            </EditDashboardModalTrigger>
            .
          </span>
        );
      }
      return (
        <Alert bsStyle="warning">
          <i className="fa fa-info-circle" />&nbsp;
          No dashboards configured. {createDashboardButton}
        </Alert>
      );
    }

    const dashboardList = this.props.dashboards.sortBy(dashboard => dashboard.title).map(this._formatDashboard);

    return (
      <ul className="streams">
        {dashboardList}
      </ul>
    );
  },
});

export default DashboardList;
