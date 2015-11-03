import React from 'react';
import Reflux from 'reflux';
import { Row, Col, Button, Alert } from 'react-bootstrap';

import CurrentUserStore from 'stores/users/CurrentUserStore';
import DashboardStore from 'stores/dashboard/DashboardStore';

import DocsHelper from 'util/DocsHelper';

import Spinner from 'components/common/Spinner';
import PermissionsMixin from 'util/PermissionsMixin';
import DocumentationLink from 'components/support/DocumentationLink';
import EditDashboardModalTrigger from 'components/dashboard/EditDashboardModalTrigger';
import Widget from 'components/widgets/Widget';
import SupportLink from 'components/support/SupportLink';

import { initializeDashboard } from 'legacy/dashboards/dashboards';

const ShowDashboardPage = React.createClass({
  propTypes: {
    dashboardId: React.PropTypes.string.isRequired,
  },
  mixins: [Reflux.connect(CurrentUserStore), PermissionsMixin],

  componentDidMount() {
    DashboardStore.get(this.props.params.dashboardId)
      .then((dashboard) => {
        this.setState({dashboard: dashboard});
      });
  },
  componentDidUpdate() {
    initializeDashboard();
  },
  DASHBOARDS_EDIT: 'dashboards:edit',
  updateUnFocussed() {
    return this.state.currentUser.preferences.updateUnfocussed;
  },
  emptyDashboard() {
    return (
      <Row className="content">
        <Col md={12}>
          <Alert className="no-widgets">
            This dashboard has no widgets yet. Learn how to add widgets in the <DocumentationLink page={DocsHelper.PAGES.DASHBOARDS} text="documentation"/>.
          </Alert>
        </Col>
      </Row>
    );
  },
  formatDashboard(dashboard) {
    if (dashboard.widgets.length === 0) {
      return this.emptyDashboard();
    }

    const widgets = this.state.dashboard.widgets.sort((d1, d2) => {
      if (d1.col === d2.col) {
        return d1.row < d2.row;
      }

      return d1.col < d2.col;
    }).map((widget) => {
      const position = dashboard.positions[widget.id] || {row: 0, col: 0, width: 1, height: 1};
      return (
        <li key={'li-' + widget.id} data-row={position.row} data-col={position.col} data-sizex={position.width} data-sizey={position.height}>
          <Widget key={'widget-' + widget.id} widgetId={widget.id} dashboardId={dashboard.id} />
        </li>
      );
    });
    return (
      <Row>
        <div className="dashboard">
          <div className="gridster" data-dashboard-id={dashboard.id}>
            <ul>
              {widgets}
            </ul>
          </div>
        </div>
      </Row>
    );
  },
  render() {
    if (!this.state.dashboard) {
      return <Spinner />;
    }

    const dashboard = this.state.dashboard;
    const currentUser = this.state.currentUser;

    const actions = dashboard.widgets.length > 0 ? (
      <span>
        <Button id="update-unfocussed" bsStyle="info" data-update-unfocussed={this.updateUnFocussed()}>
          Update in foreground
        </Button>
        {' '}
        <Button className="toggle-fullscreen" bsStyle="info">Fullscreen</Button>
        {this.isPermitted(currentUser.permissions, this.DASHBOARDS_EDIT + ':' + dashboard.id) &&
          <span>
            {' '}
            <Button id="toggle-dashboard-lock" bsStyle="success" data-locked="true">Unlock / Edit</Button>
            &nbsp;
          </span>}
      </span>) : null;

    const supportText = this.isPermitted(currentUser.permissions, this.DASHBOARDS_EDIT + ':' + dashboard.id) ?
      (<div id="drag-widgets-description">
        <SupportLink small={true}>
          Drag widgets to any position you like in <a id="unlock-dashboard" href="#" role="button">
            unlock / edit</a>{' '}
          mode.</SupportLink>
        </div>) : null;

    return (
      <span>
        <Row className="content content-head" style={{marginBottom: '5px'}}>
          <Col md={12}>
            <div className="pull-right actions">
              {actions}
            </div>
            <h1>
              <span data-dashboard-id={dashboard.id} className="dashboard-title">{dashboard.title}</span>
              <EditDashboardModalTrigger id={dashboard.id} action="edit" title={dashboard.title}
                                         description={dashboard.description} buttonClass="btn-info btn-xs">
                <i className="fa fa-pencil"/>
              </EditDashboardModalTrigger>
            </h1>

            <p className="description">
              <span data-dashboard-id={dashboard.id} className="dashboard-description">{dashboard.description}</span>
            </p>

            {supportText}

          </Col>
        </Row>
        {this.formatDashboard(dashboard)}
      </span>
    );
  }
});

export default ShowDashboardPage;
