'use strict';

var $ = require('jquery');

var React = require('react');
var DropdownButton = require('react-bootstrap').DropdownButton;
var MenuItem = require('react-bootstrap').MenuItem;
var Immutable = require('immutable');

var WidgetStore = require('../../stores/widgets/WidgetsStore');
var WidgetCreationModal = require('../widgets/WidgetCreationModal');

var AddToDashboardMenu = React.createClass({
    getInitialState() {
        return {
            dashboards: Immutable.Map(),
            selectedDashboard: ""
        };
    },
    componentDidMount() {
        $(document).trigger('get-original-search.graylog.search', {callback: this._setOriginalSearchParams});
    },
    componentWillReceiveProps(newProps) {
        this._setDashboards(newProps.dashboards);
    },
    _setDashboards(dashboards) {
        this.setState({dashboards: dashboards});
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
        widgetConfig = widgetConfig.concat(searchParams).concat(configuration);

        var promise = WidgetStore.addWidget(this.state.selectedDashboard, this.props.widgetType, title, widgetConfig.toJS());
        promise.done(() => this.refs.widgetModal.saved());
    },
    render() {
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
            <div style={{display: 'inline'}}>
                <DropdownButton bsStyle="info"
                                bsSize="small"
                                noCaret
                                title={this.props.title}
                                pullRight={this.props.pullRight}
                                onSelect={this._selectDashboard}>
                    {dashboards}
                </DropdownButton>
                <WidgetCreationModal ref="widgetModal"
                                     widgetType={this.props.widgetType}
                                     supportsTrending={true}
                                     onConfigurationSaved={this._saveWidget}/>
            </div>
        );
    }
});

module.exports = AddToDashboardMenu;