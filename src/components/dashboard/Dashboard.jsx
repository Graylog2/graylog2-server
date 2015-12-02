/* global jsRoutes */

import React from 'react';
import { DropdownButton, MenuItem } from 'react-bootstrap';

import EditDashboardModalTrigger from './EditDashboardModalTrigger';
import PermissionsMixin from '../../util/PermissionsMixin';

import DashboardsStore from '../../stores/dashboards/DashboardsStore';

import { LinkContainer } from 'react-router-bootstrap';
import Routes from 'routing/Routes';
import jsRoutes from 'routing/jsRoutes';

const Dashboard = React.createClass({
  propTypes: {
    dashboard: React.PropTypes.object,
    permissions: React.PropTypes.arrayOf(React.PropTypes.string),
  },
  mixins: [PermissionsMixin],
  _getDashboardActions() {
    let dashboardActions;

    if (this.isPermitted(this.props.permissions, [`dashboards:edit:${this.props.dashboard.id}`])) {
      dashboardActions = (
        <div className="stream-actions">
          <EditDashboardModalTrigger id={this.props.dashboard.id} action="edit" title={this.props.dashboard.title}
                                     description={this.props.dashboard.description} buttonClass="btn-info"/>
          &nbsp;
          <DropdownButton title="More actions" pullRight id={`more-actions-dropdown-${this.props.dashboard.id}`}>
            <LinkContainer to={Routes.startpage_set('dashboard', this.props.dashboard.id)}>
              <MenuItem>Set as startpage</MenuItem>
            </LinkContainer>
            <MenuItem divider/>
            <MenuItem onSelect={this._onDashboardDelete}>Delete this dashboard</MenuItem>
          </DropdownButton>
        </div>
      );
    } else {
      dashboardActions = (
        <div className="stream-actions">
          <DropdownButton title="More actions" pullRight>
            <MenuItem href={jsRoutes.controllers.StartpageController.set('dashboard', this.props.dashboard.id).url}>Set
              as startpage</MenuItem>
          </DropdownButton>
        </div>
      );
    }

    return dashboardActions;
  },
  render() {
    const createdFromContentPack = (this.props.dashboard.content_pack ?
      <i className="fa fa-cube" title="Created from content pack"/> : null);

    return (
      <li className="stream">
        <h2>
          <LinkContainer to={Routes.dashboard_show(this.props.dashboard.id)}>
            <a><span ref="dashboardTitle">{this.props.dashboard.title}</span></a>
          </LinkContainer>
        </h2>

        <div className="stream-data">
          {this._getDashboardActions()}
          <div className="stream-description">
            {createdFromContentPack}
            <span ref="dashboardDescription">{this.props.dashboard.description}</span>
          </div>
        </div>
      </li>
    );
  },
  _onDashboardDelete() {
    if (window.confirm(`Do you really want to delete the dashboard ${this.props.dashboard.title}?`)) {
      DashboardsStore.remove(this.props.dashboard);
    }
  },
});

export default Dashboard;
