import React from 'react';
import { ButtonGroup, DropdownButton, MenuItem } from 'react-bootstrap';
import Immutable from 'immutable';
import $ from 'jquery';

import DashboardsStore from 'stores/dashboards/DashboardsStore';
import WidgetStore from 'stores/widgets/WidgetsStore';

import PermissionsMixin from 'util/PermissionsMixin';
import { WidgetCreationModal } from 'components/widgets';
import { EditDashboardModal } from 'components/dashboard';

const AddToDashboardMenu = React.createClass({
  mixins: [PermissionsMixin],
  getInitialState() {
    return {
      dashboards: DashboardsStore.writableDashboards,
      selectedDashboard: '',
    };
  },
  componentDidMount() {
    DashboardsStore.addOnWritableDashboardsChangedCallback(dashboards => {
      if (this.isMounted()) {
        this.setState({dashboards: dashboards});
      }
    });
    $(document).trigger('get-original-search.graylog.search', {callback: this._setOriginalSearchParams});
  },
  _setOriginalSearchParams(originalSearchParams) {
    this.searchParams = originalSearchParams;
  },
  _selectDashboard(event, dashboardId) {
    this.setState({selectedDashboard: dashboardId});
    this.refs.widgetModal.open();
  },
  _saveWidget(title, configuration) {
    let widgetConfig = Immutable.Map(this.props.configuration);
    let searchParams = Immutable.Map(this.searchParams);
    // Changes the "relative" key used to store relative time-range to "range"
    if (searchParams.has('relative')) {
      searchParams = searchParams.set('range', searchParams.get('relative')).delete('relative');
    }
    // Stores stream ID with the right key name for the add widget request
    if (searchParams.has('streamId')) {
      searchParams = searchParams.set('stream_id', searchParams.get('streamId')).delete('streamId');
    }
    widgetConfig = searchParams.merge(widgetConfig).merge(configuration);

    const promise = WidgetStore.addWidget(this.state.selectedDashboard, this.props.widgetType, title, widgetConfig.toJS());
    promise.done(() => this.refs.widgetModal.saved());
  },
  _createNewDashboard() {
    this.refs.createDashboardModal.open();
  },
  _renderDashboardMenu() {
    let dashboards = Immutable.List();

    this.state.dashboards
      .sortBy(dashboard => dashboard.title)
      .forEach((dashboard, id) => {
        dashboards = dashboards.push(
          <MenuItem eventKey={id} key={dashboard.id} style={{textTransform: 'capitalize'}}>
            {dashboard.title}
          </MenuItem>
        );
      });

    return (
      <DropdownButton bsStyle={this.props.bsStyle || 'info'}
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
      <div style={{display: 'inline'}}>
        <DropdownButton bsStyle={this.props.bsStyle || 'info'}
                        bsSize="small"
                        title={this.props.title}
                        pullRight={this.props.pullRight}
                        onSelect={canCreateDashboard ? this._createNewDashboard : () => {}}
                        id="no-dashboards-available-dropdown">
          {option}
        </DropdownButton>
        <EditDashboardModal ref="createDashboardModal" onSaved={(id) => this._selectDashboard(undefined, id)}/>
      </div>
    );
  },
  render() {
    return (
      <div style={{display: 'inline-block'}}>
        <ButtonGroup>
          {this.props.children}

          {!this.props.hidden && (this.state.dashboards.size > 0 ?
            this._renderDashboardMenu() : this._renderNoDashboardsMenu())}
        </ButtonGroup>
        <WidgetCreationModal ref="widgetModal"
                             widgetType={this.props.widgetType}
                             supportsTrending
                             configuration={this.props.configuration}
                             onConfigurationSaved={this._saveWidget}
                             fields={this.props.fields}
                             isStreamSearch={this.props.isStreamSearch}/>
      </div>
    );
  },
});

export default AddToDashboardMenu;
