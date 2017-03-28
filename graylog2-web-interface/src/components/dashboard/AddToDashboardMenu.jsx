import React, { PropTypes } from 'react';
import { ButtonGroup, DropdownButton, MenuItem } from 'react-bootstrap';
import Immutable from 'immutable';

import StoreProvider from 'injection/StoreProvider';
const SearchStore = StoreProvider.getStore('Search');
const DashboardsStore = StoreProvider.getStore('Dashboards');
const WidgetsStore = StoreProvider.getStore('Widgets');

import PermissionsMixin from 'util/PermissionsMixin';
import { WidgetCreationModal } from 'components/widgets';
import { EditDashboardModal } from 'components/dashboard';

const AddToDashboardMenu = React.createClass({
  propTypes: {
    widgetType: PropTypes.string.isRequired,
    title: PropTypes.string.isRequired,
    permissions: PropTypes.arrayOf(PropTypes.string).isRequired,
    bsStyle: PropTypes.string,
    configuration: PropTypes.object,
    fields: PropTypes.array,
    hidden: PropTypes.bool,
    pullRight: PropTypes.bool,
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.element),
      PropTypes.element,
    ]),
  },

  mixins: [PermissionsMixin],

  getInitialState() {
    return {
      dashboards: undefined,
      selectedDashboard: '',
    };
  },

  getDefaultProps() {
    return {
      bsStyle: 'info',
      configuration: {},
      hidden: false,
      pullRight: false,
    };
  },

  componentDidMount() {
    this._initializeDashboards();
  },
  _initializeDashboards() {
    DashboardsStore.addOnWritableDashboardsChangedCallback((dashboards) => {
      if (this.isMounted()) {
        this._updateDashboards(dashboards);
      }
    });

    const dashboards = DashboardsStore.writableDashboards;
    // Trigger a dashboard update if the store haven't got any dashboards
    if (dashboards.size === 0) {
      DashboardsStore.updateWritableDashboards();
      return;
    }

    this._updateDashboards(dashboards);
  },
  _updateDashboards(newDashboards) {
    this.setState({ dashboards: newDashboards });
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
    widgetConfig = searchParams.merge(widgetConfig).merge(configuration);

    const promise = WidgetsStore.addWidget(this.state.selectedDashboard, this.props.widgetType, title, widgetConfig.toJS());
    promise.done(() => this.refs.widgetModal.saved());
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

    this.state.dashboards
      .sortBy(dashboard => dashboard.title)
      .forEach((dashboard, id) => {
        dashboards = dashboards.push(
          <MenuItem eventKey={id} key={dashboard.id}>
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
        <EditDashboardModal ref="createDashboardModal" onSaved={id => this._selectDashboard(undefined, id)} />
      </div>
    );
  },
  render() {
    let dropdownMenu;
    if (this.state.dashboards === undefined) {
      dropdownMenu = this._renderLoadingDashboardsMenu();
    } else {
      dropdownMenu = (!this.props.hidden && (this.state.dashboards.size > 0 ? this._renderDashboardMenu() : this._renderNoDashboardsMenu()));
    }

    return (
      <div style={{ display: 'inline-block' }}>
        <ButtonGroup>
          {this.props.children}
          {dropdownMenu}
        </ButtonGroup>
        <WidgetCreationModal ref="widgetModal"
                             widgetType={this.props.widgetType}
                             onConfigurationSaved={this._saveWidget}
                             fields={this.props.fields} />
      </div>
    );
  },
});

export default AddToDashboardMenu;
