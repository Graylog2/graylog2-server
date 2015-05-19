'use strict';

var $ = require('jquery');

var React = require('react');
var DropdownButton = require('react-bootstrap').DropdownButton;
var MenuItem = require('react-bootstrap').MenuItem;
var ButtonGroup = require('react-bootstrap').ButtonGroup;
var Immutable = require('immutable');

var PermissionsMixin = require('../../util/PermissionsMixin');
var WidgetCreationModal = require('../widgets/WidgetCreationModal');
var EditDashboardModal = require('./EditDashboardModal');

var DashboardStore = require('../../stores/dashboard/DashboardStore');
var WidgetStore = require('../../stores/widgets/WidgetsStore');

var AddToDashboardMenu = React.createClass({
    mixins: [PermissionsMixin],
    getInitialState() {
        return {
            dashboards: DashboardStore.dashboards,
            selectedDashboard: ""
        };
    },
    componentDidMount() {
        DashboardStore.addOnDashboardsChangedCallback(dashboards => this.setState({dashboards: dashboards}));
        $(document).trigger('get-original-search.graylog.search', {callback: this._setOriginalSearchParams});
    },
    _setOriginalSearchParams(originalSearchParams) {
        this.searchParams = originalSearchParams;
    },
    _selectDashboard(dashboardId) {
        this.setState({selectedDashboard: dashboardId});
        this.refs.widgetModal.open();
    },
    _saveWidget(title, configuration) {
        var widgetConfig = Immutable.Map(this.props.configuration);
        var searchParams = Immutable.Map(this.searchParams);
        widgetConfig = searchParams.merge(widgetConfig).merge(configuration);

        var promise = WidgetStore.addWidget(this.state.selectedDashboard, this.props.widgetType, title, widgetConfig.toJS());
        promise.done(() => this.refs.widgetModal.saved());
    },
    _createNewDashboard() {
        this.refs['createDashboardModal'].open();
    },
    _renderDashboardMenu() {
        var dashboards = Immutable.List();

        this.state.dashboards
            .sortBy(dashboard => dashboard.title)
            .forEach((dashboard, id) => {
                dashboards = dashboards.push(
                    <MenuItem eventKey={id} key={dashboard.title} style={{textTransform: 'capitalize'}}>
                        {dashboard.title}
                    </MenuItem>
                );
            });

        return (
            <DropdownButton bsStyle={this.props.bsStyle || "info"}
                            bsSize="small"
                            title={this.props.title}
                            pullRight={this.props.pullRight}
                            onSelect={this._selectDashboard}>
                {dashboards}
            </DropdownButton>
        );
    },
    _renderNoDashboardsMenu() {
        var canCreateDashboard = this.isPermitted(this.props.permissions, ["dashboards:create"]);
        var option;
        if (canCreateDashboard) {
            option = <MenuItem key="createDashboard">No dashboards, create one?</MenuItem>;
        } else {
            option = <MenuItem key="noDashboards">No dashboards available</MenuItem>;
        }

        return (
            <div style={{display: 'inline'}}>
                <DropdownButton bsStyle={this.props.bsStyle || "info"}
                                bsSize="small"
                                title={this.props.title}
                                pullRight={this.props.pullRight}
                                onSelect={canCreateDashboard ? this._createNewDashboard : () => {}}>
                    {option}
                </DropdownButton>
                <EditDashboardModal ref="createDashboardModal" onSaved={(id) => this._selectDashboard(id)}/>
            </div>
        );
    },
    render() {
        return (
            <div style={{display: 'inline'}}>
                <ButtonGroup>
                    {this.props.children}
                    {this.state.dashboards.size > 0 ?
                        this._renderDashboardMenu() : this._renderNoDashboardsMenu()}
                </ButtonGroup>
                <WidgetCreationModal ref="widgetModal"
                                     widgetType={this.props.widgetType}
                                     supportsTrending={true}
                                     configuration={this.props.configuration}
                                     onConfigurationSaved={this._saveWidget}
                                     fields={this.props.fields}/>
            </div>
        );
    }
});

module.exports = AddToDashboardMenu;