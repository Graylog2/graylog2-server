import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { ButtonGroup, ButtonToolbar, DropdownButton, MenuItem } from 'react-bootstrap';
import Immutable from 'immutable';

import CombinedProvider from 'injection/CombinedProvider';
import StoreProvider from 'injection/StoreProvider';

const SearchStore = StoreProvider.getStore('Search');
const { DashboardsActions, DashboardsStore } = CombinedProvider.get('Dashboards');
const WidgetsStore = StoreProvider.getStore('Widgets');

import PermissionsMixin from 'util/PermissionsMixin';
import { WidgetCreationModal } from 'components/widgets';
import { EditDashboardModal } from 'components/dashboard';

import style from './AddToDashboardMenu.css';

const AddToDashboardMenu = createReactClass({
  displayName: 'AddToDashboardMenu',

  propTypes: {
    widgetType: PropTypes.string.isRequired,
    title: PropTypes.string.isRequired,
    permissions: PropTypes.arrayOf(PropTypes.string).isRequired,
    bsStyle: PropTypes.string,
    configuration: PropTypes.object,
    fields: PropTypes.array,
    hidden: PropTypes.bool,
    pullRight: PropTypes.bool,
    appendMenus: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.element),
      PropTypes.element,
    ]),
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.element),
      PropTypes.element,
    ]),
  },

  mixins: [Reflux.connect(DashboardsStore), PermissionsMixin],

  getInitialState() {
    return {
      selectedDashboard: '',
      loading: false,
    };
  },

  getDefaultProps() {
    return {
      bsStyle: 'default',
      configuration: {},
      hidden: false,
      pullRight: false,
    };
  },

  _selectDashboard(dashboardId) {
    this.setState({ selectedDashboard: dashboardId });
    this.refs.widgetModal.open();
  },

  _saveWidget(title, configuration) {
    let widgetConfig = Immutable.Map(this.props.configuration);
    let searchParams = Immutable.Map(SearchStore.getOriginalSearchParams());
    if (searchParams.has('range_type')) {
      switch (searchParams.get('range_type')) {
        case 'relative':
          const relativeTimeRange = Immutable.Map({
            // Changes the "relative" key used to store relative time-range to "range"
            range: searchParams.get('relative'),
            type: 'relative',
          });
          searchParams = searchParams
            .set('timerange', relativeTimeRange)
            .delete('relative')
            .delete('range_type');
          break;
        case 'absolute':
          const from = searchParams.get('from');
          const to = searchParams.get('to');
          const absoluteTimeRange = Immutable.Map({
            type: 'absolute',
            from: from,
            to: to,
          });
          searchParams = searchParams
            .set('timerange', absoluteTimeRange)
            .delete('from')
            .delete('to')
            .delete('range_type');
          break;
        case 'keyword':
          const keywordTimeRange = Immutable.Map({
            type: 'keyword',
            keyword: searchParams.get('keyword'),
          });
          searchParams = searchParams
            .set('timerange', keywordTimeRange)
            .delete('keyword')
            .delete('range_type');
      }
    }
    // Stores stream ID with the right key name for the add widget request
    if (searchParams.has('streamId')) {
      searchParams = searchParams.set('stream_id', searchParams.get('streamId')).delete('streamId');
    }

    if (widgetConfig.has('series')) {
      // If widget has several series, each of them will contain a query, delete it from global widget configuration.
      searchParams = searchParams.delete('query');
    }

    widgetConfig = searchParams.merge(widgetConfig).merge(configuration);

    this.setState({ loading: true });
    const promise = WidgetsStore.addWidget(this.state.selectedDashboard, this.props.widgetType, title, widgetConfig.toJS());
    promise
      .then(() => this.refs.widgetModal.saved())
      .finally(() => this.setState({ loading: false }));
  },

  _createNewDashboard() {
    this.refs.createDashboardModal.open();
  },

  _renderLoadingDashboardsMenu() {
    return (
      <DropdownButton bsStyle={this.props.bsStyle}
                      bsSize="small"
                      title={this.props.title}
                      pullRight={this.props.pullRight}
                      id="dashboard-selector-dropdown">
        <MenuItem disabled>Loading dashboards...</MenuItem>
      </DropdownButton>
    );
  },

  _renderDashboardMenu() {
    let dashboards = Immutable.List();

    this.state.writableDashboards
      .sortBy(dashboard => dashboard.title)
      .forEach((dashboard) => {
        dashboards = dashboards.push(
          <MenuItem eventKey={dashboard.id} key={dashboard.id}>
            {dashboard.title}
          </MenuItem>,
        );
      });

    return (
      <DropdownButton bsStyle={this.props.bsStyle}
                      bsSize="small"
                      title={this.props.title}
                      pullRight={this.props.pullRight}
                      onSelect={this._selectDashboard}
                      id="dashboard-selector-dropdown">
        {dashboards}
      </DropdownButton>
    );
  },

  _renderNoDashboardsMenu() {
    const canCreateDashboard = this.isPermitted(this.props.permissions, ['dashboards:create']);
    let option;
    if (canCreateDashboard) {
      option = <MenuItem key="createDashboard">No dashboards, create one?</MenuItem>;
    } else {
      option = <MenuItem key="noDashboards">No dashboards available</MenuItem>;
    }

    return (
      <div style={{ display: 'inline' }}>
        <DropdownButton bsStyle={this.props.bsStyle}
                        bsSize="small"
                        title={this.props.title}
                        pullRight={this.props.pullRight}
                        onSelect={canCreateDashboard ? this._createNewDashboard : () => {}}
                        id="no-dashboards-available-dropdown">
          {option}
        </DropdownButton>
        <EditDashboardModal ref="createDashboardModal" onSaved={this._selectDashboard} />
      </div>
    );
  },

  render() {
    let addToDashboardMenu;
    if (this.state.writableDashboards === undefined) {
      addToDashboardMenu = this._renderLoadingDashboardsMenu();
    } else {
      addToDashboardMenu = (!this.props.hidden && (this.state.writableDashboards.size > 0 ? this._renderDashboardMenu() : this._renderNoDashboardsMenu()));
    }

    const { appendMenus, children } = this.props;
    const loading = this.state.loading;

    return (
      <div style={{ display: 'inline-block' }}>
        <ButtonToolbar className={style.toolbar}>
          <ButtonGroup>
            {addToDashboardMenu}
            {appendMenus}
          </ButtonGroup>

          {children}
        </ButtonToolbar>
        <WidgetCreationModal ref="widgetModal"
                             widgetType={this.props.widgetType}
                             onConfigurationSaved={this._saveWidget}
                             fields={this.props.fields}
                             loading={loading} />
      </div>
    );
  },
});

export default AddToDashboardMenu;
