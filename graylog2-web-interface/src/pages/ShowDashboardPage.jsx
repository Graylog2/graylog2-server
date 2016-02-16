import React from 'react';
import Reflux from 'reflux';
import { Row, Col, Button, Alert } from 'react-bootstrap';

import CurrentUserStore from 'stores/users/CurrentUserStore';
import DashboardsStore from 'stores/dashboards/DashboardsStore';
import FocusStore from 'stores/tools/FocusStore';
import WidgetsStore from 'stores/widgets/WidgetsStore';

import DocsHelper from 'util/DocsHelper';
import UserNotification from 'util/UserNotification';

import { GridsterContainer, PageHeader, Spinner, IfPermitted } from 'components/common';
import PermissionsMixin from 'util/PermissionsMixin';
import DocumentationLink from 'components/support/DocumentationLink';
import EditDashboardModalTrigger from 'components/dashboard/EditDashboardModalTrigger';
import Widget from 'components/widgets/Widget';

const ShowDashboardPage = React.createClass({
  mixins: [Reflux.connect(CurrentUserStore), Reflux.connect(FocusStore), PermissionsMixin],

  getInitialState() {
    return {
      locked: true,
      forceUpdateInBackground: false,
    };
  },
  componentDidMount() {
    this.loadData();
    this.listenTo(WidgetsStore, this.removeWidget);
    this.loadInterval = setInterval(this.loadData, 2000);
  },
  componentWillUnmount() {
    if (this.loadInterval) {
      clearInterval(this.loadInterval);
    }
  },
  loadData() {
    DashboardsStore.get(this.props.params.dashboardId)
      .then((dashboard) => {
        if (this.isMounted()) {
          this.setState({dashboard: dashboard});
        }
      });
  },
  DASHBOARDS_EDIT: 'dashboards:edit',
  updateUnFocussed() {
    return this.state.currentUser.preferences.updateUnfocussed;
  },
  shouldUpdate() {
    return Boolean(this.updateUnFocussed() || this.state.forceUpdateInBackground || this.state.focus);
  },
  removeWidget(props) {
    if (props.delete) {
      this.loadData();
    }
  },
  emptyDashboard() {
    return (
      <Row className="content">
        <Col md={12}>
          <Alert className="no-widgets">
            This dashboard has no widgets yet. Learn how to add widgets in the <DocumentationLink
            page={DocsHelper.PAGES.DASHBOARDS} text="documentation"/>.
          </Alert>
        </Col>
      </Row>
    );
  },
  _defaultWidgetDimensions(widget) {
    const dimensions = {col: 0, row: 0};

    switch (widget.type.toUpperCase()) {
      case Widget.Type.SEARCH_RESULT_CHART:
      case Widget.Type.STACKED_CHART:
      case Widget.Type.FIELD_CHART:
        dimensions.width = 2;
        dimensions.height = 1;
        break;
      case Widget.Type.QUICKVALUES:
        dimensions.width = 1;
        dimensions.height = (widget.config.show_pie_chart && widget.config.show_data_table ? 3 : 2);
        break;
      case Widget.Type.SEARCH_RESULT_COUNT:
      case Widget.Type.STREAM_SEARCH_RESULT_COUNT:
      case Widget.Type.STATS_COUNT:
      default:
        dimensions.width = 1;
        dimensions.height = 1;
    }

    return dimensions;
  },
  _dashboardIsEmpty(dashboard) {
    return dashboard.widgets.length === 0;
  },
  formatDashboard(dashboard) {
    if (this._dashboardIsEmpty(dashboard)) {
      return this.emptyDashboard();
    }

    const positions = {};
    dashboard.widgets.forEach(widget => {
      positions[widget.id] = dashboard.positions[widget.id] || this._defaultWidgetDimensions(widget);
    });

    const widgets = dashboard.widgets.sort((widget1, widget2) => {
      const position1 = positions[widget1.id];
      const position2 = positions[widget2.id];
      if (position1.col === position2.col) {
        return position1.row - position2.row;
      }

      return position1.col - position2.col;
    }).map((widget) => {
      return (
        <Widget id={widget.id} key={'widget-' + widget.id} widget={widget} dashboardId={dashboard.id}
                locked={this.state.locked} shouldUpdate={this.shouldUpdate()}/>
      );
    });

    return (
      <Row>
        <div className="dashboard">
          <GridsterContainer ref="gridsterContainer" positions={positions} onPositionsChange={this._onPositionsChange}>
            {widgets}
          </GridsterContainer>
        </div>
      </Row>
    );
  },
  _unlockDashboard(event) {
    event.preventDefault();
    this.setState({locked: false});
  },
  _onUnlock() {
    const locked = !this.state.locked;
    this.setState({locked: locked});

    if (locked) {
      this.refs.gridsterContainer.lockGrid();
    } else {
      this.refs.gridsterContainer.unlockGrid();
    }
  },
  _onPositionsChange(newPositions) {
    DashboardsStore.updatePositions(this.state.dashboard, newPositions);
  },
  _toggleFullscreen() {
    const element = document.documentElement;
    if (element.requestFullscreen) {
      element.requestFullscreen();
    } else if (element.mozRequestFullScreen) {
      element.mozRequestFullScreen();
    } else if (element.webkitRequestFullscreen) {
      element.webkitRequestFullscreen();
    } else if (element.msRequestFullscreen) {
      element.msRequestFullscreen();
    }
  },
  _toggleUpdateInBackground() {
    const forceUpdate = !this.state.forceUpdateInBackground;
    this.setState({forceUpdateInBackground: forceUpdate});
    UserNotification.success('Graphs will be updated ' + (forceUpdate ? 'even' : 'only')
      + ' when the browser is in the ' + (forceUpdate ? 'background' : 'foreground'), '');
  },
  render() {
    if (!this.state.dashboard) {
      return <Spinner />;
    }

    const dashboard = this.state.dashboard;

    let actions;
    if (!this._dashboardIsEmpty(dashboard)) {
      actions = (
        <span className="pull-right">
          <Button id="update-unfocussed" bsStyle="info" onClick={this._toggleUpdateInBackground}>
            Update in {this.state.forceUpdateInBackground ? 'foreground' : 'background'}
          </Button>
          {' '}
          <Button className="toggle-fullscreen" bsStyle="info" onClick={this._toggleFullscreen}>Fullscreen</Button>
          <IfPermitted permissions={`${this.DASHBOARDS_EDIT}:${dashboard.id}`}>
            {' '}
            <Button bsStyle="success" onClick={this._onUnlock}>{this.state.locked ? 'Unlock / Edit' : 'Lock'}</Button>
          </IfPermitted>
        </span>
      );
    }

    let supportText;
    if (!this._dashboardIsEmpty(dashboard)) {
      supportText = (
        <IfPermitted permissions={`${this.DASHBOARDS_EDIT}:${dashboard.id}`}>
          <div id="drag-widgets-description">
            Drag widgets to any position you like in <a href="#" role="button" onClick={this._unlockDashboard}>
            unlock / edit</a> mode.
          </div>
        </IfPermitted>
      );
    }

    const dashboardTitle = (
      <span>
        <span data-dashboard-id={dashboard.id} className="dashboard-title">{dashboard.title}</span>
        &nbsp;
        {!this.state.locked && !this._dashboardIsEmpty(dashboard) &&
        <EditDashboardModalTrigger id={dashboard.id} action="edit" title={dashboard.title}
                                   description={dashboard.description} buttonClass="btn-info btn-xs">
          <i className="fa fa-pencil"/>
        </EditDashboardModalTrigger>}
      </span>
    );
    return (
      <span>
        <PageHeader title={dashboardTitle} titleSize={8} buttonSize={4} buttonStyle={{textAlign: 'center'}}>
          <span data-dashboard-id={dashboard.id} className="dashboard-description">{dashboard.description}</span>
          {supportText}
          {actions}
        </PageHeader>

        {this.formatDashboard(dashboard)}
      </span>
    );
  },
});

export default ShowDashboardPage;
