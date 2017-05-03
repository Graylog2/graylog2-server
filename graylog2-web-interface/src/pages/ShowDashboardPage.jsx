import React from 'react';
import Reflux from 'reflux';
import { Row, Col, Button, Alert } from 'react-bootstrap';
import { PluginStore } from 'graylog-web-plugin/plugin';
import deepEqual from 'deep-equal';

import StoreProvider from 'injection/StoreProvider';
const StreamsStore = StoreProvider.getStore('Streams');
const CurrentUserStore = StoreProvider.getStore('CurrentUser');
const DashboardsStore = StoreProvider.getStore('Dashboards');
const FocusStore = StoreProvider.getStore('Focus');
const WidgetsStore = StoreProvider.getStore('Widgets');

import DocsHelper from 'util/DocsHelper';
import UserNotification from 'util/UserNotification';
import Routes from 'routing/Routes';

import { DocumentTitle, ReactGridContainer, PageHeader, Spinner, IfPermitted } from 'components/common';
import PermissionsMixin from 'util/PermissionsMixin';
import DocumentationLink from 'components/support/DocumentationLink';
import EditDashboardModalTrigger from 'components/dashboard/EditDashboardModalTrigger';
import Widget from 'components/widgets/Widget';

import style from './ShowDashboardPage.css';

const ShowDashboardPage = React.createClass({
  propTypes: {
    history: React.PropTypes.object.isRequired,
    params: React.PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(CurrentUserStore), Reflux.connect(FocusStore), PermissionsMixin],

  getInitialState() {
    return {
      locked: true,
      forceUpdateInBackground: false,
      streamIds: null,
    };
  },
  componentDidMount() {
    this.loadData();
    this.listenTo(WidgetsStore, this.removeWidget);
    // we use the stream ids to potentially disable search replay buttons for deleted streams
    StreamsStore.load((streams) => {
      const streamIds2 = streams.reduce((streamIds, stream) => {
        // eslint-disable-next-line no-param-reassign
        streamIds[stream.id] = stream.id;
        return streamIds;
      }, {});
      this.setState({ streamIds: streamIds2 });
    });
    this.loadInterval = setInterval(this.loadData, 2000);
    // eslint-disable-next-line react/no-did-mount-set-state
    this.setState({ forceUpdateInBackground: this.state.currentUser.preferences.updateUnfocussed });
  },
  componentWillUnmount() {
    if (this.loadInterval) {
      clearInterval(this.loadInterval);
    }
  },
  DASHBOARDS_EDIT: 'dashboards:edit',
  DEFAULT_HEIGHT: 1,
  DEFAULT_WIDTH: 2,
  loadData() {
    const dashboardId = this.props.params.dashboardId;
    DashboardsStore.get(dashboardId)
      .then((dashboard) => {
        if (!this.isMounted()) {
          return;
        }

        // Compare dashboard in state with the one received, need to sort widgets to avoid that they come in
        // a different order, affecting the comparison.
        dashboard.widgets.sort((w1, w2) => w1.id.localeCompare(w2.id));
        if (!this.state.dashboard || !deepEqual(this.state.dashboard, dashboard)) {
          this.setState({ dashboard: dashboard });
        }
      }, (response) => {
        if (response.additional && response.additional.status === 404) {
          UserNotification.error(`Unable to find a dashboard with the id <${dashboardId}>. Maybe it was deleted in the meantime.`);
          this.props.history.pushState(null, Routes.DASHBOARDS);
        }
      });
  },
  shouldUpdate() {
    return Boolean(this.state.forceUpdateInBackground || this.state.focus);
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
            page={DocsHelper.PAGES.DASHBOARDS} text="documentation" />.
          </Alert>
        </Col>
      </Row>
    );
  },
  _defaultWidgetDimensions(widget) {
    const dimensions = { col: 0, row: 0 };

    const widgetPlugin = PluginStore.exports('widgets').filter(plugin => plugin.type.toUpperCase() === widget.type.toUpperCase())[0];
    if (widgetPlugin) {
      dimensions.height = widgetPlugin.defaultHeight;
      dimensions.width = widgetPlugin.defaultWidth;
    } else {
      dimensions.height = this.DEFAULT_HEIGHT;
      dimensions.width = this.DEFAULT_WIDTH;
    }

    return dimensions;
  },
  _dashboardIsEmpty(dashboard) {
    return dashboard.widgets.length === 0;
  },
  _validDimension(dimension) {
    return dimension !== null && dimension !== undefined && dimension !== 0;
  },
  formatDashboard(dashboard) {
    if (this._dashboardIsEmpty(dashboard)) {
      return this.emptyDashboard();
    }

    const positions = {};
    dashboard.widgets.forEach((widget) => {
      const persistedDimensions = dashboard.positions[widget.id] || {};
      const defaultDimensions = this._defaultWidgetDimensions(widget);
      positions[widget.id] = {
        col: (persistedDimensions.col === undefined ? defaultDimensions.col : persistedDimensions.col),
        row: (persistedDimensions.row === undefined ? defaultDimensions.row : persistedDimensions.row),
        height: (this._validDimension(persistedDimensions.height) ? persistedDimensions.height : defaultDimensions.height),
        width: (this._validDimension(persistedDimensions.width) ? persistedDimensions.width : defaultDimensions.width),
      };
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
        <div key={widget.id} className={style.widgetContainer}>
          <Widget id={widget.id} key={`widget-${widget.id}`} widget={widget} dashboardId={dashboard.id}
                  locked={this.state.locked} shouldUpdate={this.shouldUpdate()} streamIds={this.state.streamIds} />
        </div>
      );
    });

    return (
      <Row>
        <div className="dashboard">
          <ReactGridContainer positions={positions} onPositionsChange={this._onPositionsChange} locked={this.state.locked}>
            {widgets}
          </ReactGridContainer>
        </div>
      </Row>
    );
  },
  _unlockDashboard(event) {
    event.preventDefault();
    if (this.state.locked) {
      this._toggleUnlock();
    }
  },
  _toggleUnlock() {
    this.setState({ locked: !this.state.locked });
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
    this.setState({ forceUpdateInBackground: forceUpdate });
    UserNotification.success(`Graphs will be updated ${forceUpdate ? 'even' : 'only'} when the browser is in the ${forceUpdate ? 'background' : 'foreground'}`, '');
  },
  render() {
    if (!this.state.dashboard) {
      return <Spinner />;
    }

    const dashboard = this.state.dashboard;

    let actions;
    if (!this._dashboardIsEmpty(dashboard)) {
      actions = (
        <div>
          <Button id="update-unfocussed" bsStyle="info" onClick={this._toggleUpdateInBackground}>
            Update in {this.state.forceUpdateInBackground ? 'foreground' : 'background'}
          </Button>
          {' '}
          <Button className="toggle-fullscreen" bsStyle="info" onClick={this._toggleFullscreen}>Fullscreen</Button>
          <IfPermitted permissions={`${this.DASHBOARDS_EDIT}:${dashboard.id}`}>
            {' '}
            <Button bsStyle="success" onClick={this._toggleUnlock}>{this.state.locked ? 'Unlock / Edit' : 'Lock'}</Button>
          </IfPermitted>
        </div>
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

    const editDashboardTrigger = !this.state.locked && !this._dashboardIsEmpty(dashboard) ?
      (<EditDashboardModalTrigger id={dashboard.id} action="edit" title={dashboard.title}
                                 description={dashboard.description} buttonClass="btn-info btn-xs">
        <i className="fa fa-pencil" />
      </EditDashboardModalTrigger>) : null;
    const dashboardTitle = (
      <span>
        <span data-dashboard-id={dashboard.id} className="dashboard-title">{dashboard.title}</span>
        &nbsp;
        {editDashboardTrigger}
      </span>
    );
    return (
      <DocumentTitle title={`Dashboard ${dashboard.title}`}>
        <span>
          <PageHeader title={dashboardTitle}>
            <span data-dashboard-id={dashboard.id} className="dashboard-description">{dashboard.description}</span>
            {supportText}
            {actions}
          </PageHeader>

          {this.formatDashboard(dashboard)}
          <div className="clearfix" />
        </span>
      </DocumentTitle>
    );
  },
});

export default ShowDashboardPage;
