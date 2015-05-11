'use strict';

var $ = require('jquery');

var React = require('react');
var DropdownButton = require('react-bootstrap').DropdownButton;
var MenuItem = require('react-bootstrap').MenuItem;
var ButtonGroup = require('react-bootstrap').ButtonGroup;
var Immutable = require('immutable');

var WidgetStore = require('../../stores/widgets/WidgetsStore');
var WidgetCreationModal = require('../widgets/WidgetCreationModal');

var AddToDashboardMenu = React.createClass({
    getInitialState() {
        return {
            selectedDashboard: ""
        };
    },
    componentDidMount() {
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
    render() {
        var dashboards = Immutable.List();

        Immutable.Map(this.props.dashboards)
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
                <ButtonGroup>
                    {this.props.children}
                    <DropdownButton bsStyle={this.props.bsStyle || "info"}
                                    bsSize="small"
                                    title={this.props.title}
                                    pullRight={this.props.pullRight}
                                    onSelect={this._selectDashboard}>
                        {dashboards}
                    </DropdownButton>
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