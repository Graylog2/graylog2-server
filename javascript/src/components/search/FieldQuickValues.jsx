'use strict';

var React = require('react');
var Button = require('react-bootstrap').Button;

var QuickValuesVisualization = require('../visualizations/QuickValuesVisualization');
var AddToDashboardMenu = require('../dashboard/AddToDashboardMenu');
var Widget = require('../widgets/Widget');
var Spinner = require('../common/Spinner');

var FieldQuickValuesStore = require('../../stores/field-analyzers/FieldQuickValuesStore');

var FieldQuickValues = React.createClass({
    getInitialState() {
        return {
            field: undefined,
            autoReload: true,
            data: []
        };
    },

    componentDidMount() {
        this.updateIntervalId = window.setInterval(() => this._loadQuickValuesData(), 3000);
    },
    componentWillUnmount() {
        window.clearInterval(this.updateIntervalId);
    },
    _toggleAutoReload() {
        var shouldAutoReload = !this.state.autoReload;
        this.setState({autoReload: shouldAutoReload});
    },
    addFieldQuickValues(field) {
        this.setState({field: field}, this._loadQuickValuesData);
    },
    _loadQuickValuesData() {
        if (this.state.field !== undefined && this.state.autoReload) {
            this.setState({loadPending: true});
            var promise = FieldQuickValuesStore.getQuickValues(this.state.field);
            promise.done((data) => this.setState({data: data, loadPending: false}));
        }
    },
    _resetStatus() {
        this.setState(this.getInitialState());
    },
    render() {
        var content;

        var inner;
        if (this.state.data.length === 0) {
            inner = <Spinner />;
        } else {
            inner = <QuickValuesVisualization id={this.state.field}
                                      horizontal={true}
                                      config={{show_pie_chart: true, show_data_table: true}}
                                      data={this.state.data}/>;
        }

        if (this.state.field !== undefined) {
            content = (
                <div className="content-col">
                    <div className="pull-right">
                        <AddToDashboardMenu title='Add to dashboard'
                                            widgetType={Widget.Type.QUICKVALUES}
                                            configuration={{field: this.state.field}}
                                            bsStyle='default'
                                            pullRight={true}
                                            permissions={this.props.permissions}>
                            <Button bsSize='small' onClick={() => this._toggleAutoReload()}>{this.state.autoReload ? "Stop reloading" : "Reload automatically"} </Button>
                            <Button bsSize='small' onClick={() => this._resetStatus()}>Dismiss</Button>
                        </AddToDashboardMenu>
                    </div>
                    <h1>Field Statistics {this.state.loadPending && <i className="fa fa-spin fa-spinner"></i>}</h1>

                    <div>{inner}</div>
                </div>
            );
        }
        return <div id="field-quick-values">{content}</div>;
    }
});

module.exports = FieldQuickValues;