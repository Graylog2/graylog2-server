/* global jsRoutes */

const React = require('react');
const DropdownButton = require('react-bootstrap').DropdownButton;
const MenuItem = require('react-bootstrap').MenuItem;

const EditDashboardModalTrigger = require('./EditDashboardModalTrigger');
const PermissionsMixin = require('../../util/PermissionsMixin');

const DashboardStore = require('../../stores/dashboard/DashboardStore');

const Dashboard = React.createClass({
  mixins: [PermissionsMixin],
  render() {
    let dashboardActions;

    if (this.isPermitted(this.props.permissions, [`dashboards:edit:${this.props.dashboard.id}`])) {
      dashboardActions = (
        <div className="stream-actions">
          <EditDashboardModalTrigger id={this.props.dashboard.id} action="edit" title={this.props.dashboard.title}
                                     description={this.props.dashboard.description} buttonClass="btn-info"/>
          &nbsp;
          <DropdownButton title="More actions" pullRight>
            <MenuItem href={jsRoutes.controllers.StartpageController.set("dashboard", this.props.dashboard.id).url}>Set as startpage</MenuItem>
            <MenuItem divider/>
            <MenuItem onSelect={this._onDashboardDelete}>Delete this dashboard</MenuItem>
          </DropdownButton>
        </div>
      );
    }

    const createdFromContentPack = (this.props.dashboard.content_pack ?
      <i className="fa fa-cube" title="Created from content pack"></i> : null);

    return (
      <li className="stream">
        <h2>
          <a href={jsRoutes.controllers.DashboardsController.show(this.props.dashboard.id).url}>
            <span ref="dashboardTitle">{this.props.dashboard.title}</span>
          </a>
        </h2>

        <div className="stream-data">
          {dashboardActions}
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
      DashboardStore.remove(this.props.dashboard);
    }
  },
});

module.exports = Dashboard;
