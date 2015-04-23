/* global originalUniversalSearchSettings */

'use strict';

var React = require('react');
var DropdownButton = require('react-bootstrap').DropdownButton;
var MenuItem = require('react-bootstrap').MenuItem;
var Immutable = require('immutable');

var WidgetCreationModal = require('../widgets/WidgetCreationModal');

var AddToDashboardMenu = React.createClass({
    getInitialState() {
        return {
            dashboards: Immutable.Map()
        };
    },
    componentDidMount() {
        this._setDashboards(this.props.dashboards);
    },
    componentWillReceiveProps(newProps) {
        this._setDashboards(newProps.dashboards);
    },
    _setDashboards(dashboards) {
        this.setState({dashboards: dashboards});
    },
    _configureWidget(dashboardId) {
        this.refs.widgetModal.open();
    },
    _saveWidget(configuration) {
        var params = Immutable.Map(originalUniversalSearchSettings());
        var propConfigurationMap = Immutable.Map(this.props.configuration);
        params = params.concat(propConfigurationMap).concat(configuration);
        console.log(params.toJS());
    },
    render() {
        var dashboards = Immutable.List();

        this.state.dashboards
            .sortBy(dashboard => dashboard.title)
            .forEach((dashboard, id) => {
                dashboards = dashboards.push(
                    <MenuItem eventKey={id} key={dashboard.title}>{dashboard.title}</MenuItem>
                );
            });

        return (
            <div style={{display: 'inline'}}>
                <DropdownButton bsStyle="info"
                                bsSize="small"
                                noCaret
                                title={this.props.title}
                                pullRight={this.props.pullRight}
                                onSelect={this._configureWidget}>
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