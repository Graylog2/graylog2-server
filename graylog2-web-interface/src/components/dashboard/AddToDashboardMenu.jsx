import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { ButtonGroup, ButtonToolbar, DropdownButton, MenuItem } from 'components/graylog';
import Immutable from 'immutable';

import CombinedProvider from 'injection/CombinedProvider';
import StoreProvider from 'injection/StoreProvider';

import PermissionsMixin from 'util/PermissionsMixin';
import { WidgetCreationModal } from 'components/widgets';
import { EditDashboardModal } from 'components/dashboard';

import style from './AddToDashboardMenu.css';

const SearchStore = StoreProvider.getStore('Search');
const { DashboardsStore } = CombinedProvider.get('Dashboards');
const WidgetsStore = StoreProvider.getStore('Widgets');

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

  getDefaultProps() {
    return {
      appendMenus: null,
      bsStyle: 'default',
      children: null,
      configuration: {},
      fields: [],
      hidden: false,
      pullRight: false,
    };
  },

  getInitialState() {
    return {
      selectedDashboard: '',
      loading: false,
    };
  },

  _selectDashboard(dashboardId) {
    this.setState({ selectedDashboard: dashboardId });
    this.widgetModal.open();
  },

  _saveWidget(title, configuration) {
    const { configuration: configuration1, widgetType } = this.props;
    let widgetConfig = Immutable.Map(configuration1);
    let searchParams = Immutable.Map(SearchStore.getOriginalSearchParams());
    if (searchParams.has('range_type')) {
      switch (searchParams.get('range_type')) {
        case 'relative':
          searchParams = searchParams
            .set('timerange', Immutable.Map({
              // Changes the "relative" key used to store relative time-range to "range"
              range: searchParams.get('relative'),
              type: 'relative',
            }))
            .delete('relative')
            .delete('range_type');
          break;
        case 'absolute':
          searchParams = searchParams
            .set('timerange', Immutable.Map({
              type: 'absolute',
              from: searchParams.get('from'),
              to: searchParams.get('to'),
            }))
            .delete('from')
            .delete('to')
            .delete('range_type');
          break;
        case 'keyword':
          searchParams = searchParams
            .set('timerange', Immutable.Map({
              type: 'keyword',
              keyword: searchParams.get('keyword'),
            }))
            .delete('keyword')
            .delete('range_type');
          break;
        default:
          throw new Error(`Invalid time range type specified: ${searchParams.get('range_type')}`);
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
    const { selectedDashboard } = this.state;
    const promise = WidgetsStore.addWidget(selectedDashboard, widgetType, title, widgetConfig.toJS());
    promise
      .then(() => this.widgetModal.saved())
      .finally(() => this.setState({ loading: false }));
  },

  _createNewDashboard() {
    this.createDashboardModal.open();
  },

  _renderLoadingDashboardsMenu() {
    const { pullRight, bsStyle, title } = this.props;
    return (
      <DropdownButton bsStyle={bsStyle}
                      bsSize="small"
                      title={title}
                      pullRight={pullRight}
                      id="dashboard-selector-dropdown">
        <MenuItem disabled>Loading dashboards...</MenuItem>
      </DropdownButton>
    );
  },

  _renderDashboardMenu() {
    let dashboards = Immutable.List();

    const { writableDashboards } = this.state;
    const { pullRight, bsStyle, title } = this.props;

    writableDashboards
      .sortBy(dashboard => dashboard.title)
      .forEach((dashboard) => {
        dashboards = dashboards.push(
          <MenuItem eventKey={dashboard.id} key={dashboard.id}>
            {dashboard.title}
          </MenuItem>,
        );
      });

    return (
      <DropdownButton bsStyle={bsStyle}
                      bsSize="small"
                      title={title}
                      pullRight={pullRight}
                      onSelect={this._selectDashboard}
                      id="dashboard-selector-dropdown">
        {dashboards}
      </DropdownButton>
    );
  },

  _renderNoDashboardsMenu() {
    const { pullRight, permissions, bsStyle, title } = this.props;
    const canCreateDashboard = this.isPermitted(permissions, ['dashboards:create']);
    let option;
    if (canCreateDashboard) {
      option = <MenuItem key="createDashboard">No dashboards, create one?</MenuItem>;
    } else {
      option = <MenuItem key="noDashboards">No dashboards available</MenuItem>;
    }

    return (
      <div style={{ display: 'inline' }}>
        <DropdownButton bsStyle={bsStyle}
                        bsSize="small"
                        title={title}
                        pullRight={pullRight}
                        onSelect={canCreateDashboard ? this._createNewDashboard : () => {}}
                        id="no-dashboards-available-dropdown">
          {option}
        </DropdownButton>
        <EditDashboardModal ref={(createDashboardModal) => { this.createDashboardModal = createDashboardModal; }} onSaved={this._selectDashboard} />
      </div>
    );
  },

  render() {
    let addToDashboardMenu;
    const { writableDashboards } = this.state;
    const { fields, hidden, widgetType } = this.props;
    if (writableDashboards === undefined) {
      addToDashboardMenu = this._renderLoadingDashboardsMenu();
    } else {
      addToDashboardMenu = (!hidden && (writableDashboards.size > 0 ? this._renderDashboardMenu() : this._renderNoDashboardsMenu()));
    }

    const { appendMenus, children } = this.props;
    const { loading } = this.state;

    return (
      <div style={{ display: 'inline-block' }}>
        <ButtonToolbar className={style.toolbar}>
          <ButtonGroup>
            {addToDashboardMenu}
            {appendMenus}
          </ButtonGroup>

          {children}
        </ButtonToolbar>
        <WidgetCreationModal ref={(widgetModal) => { this.widgetModal = widgetModal; }}
                             widgetType={widgetType}
                             onConfigurationSaved={this._saveWidget}
                             fields={fields}
                             loading={loading} />
      </div>
    );
  },
});

export default AddToDashboardMenu;
