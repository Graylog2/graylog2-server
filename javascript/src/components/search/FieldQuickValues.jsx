'use strict';

var React = require('react');
var Button = require('react-bootstrap').Button;

var QuickValuesVisualization = require('../visualizations/QuickValuesVisualization');
var AddToDashboardMenu = require('../dashboard/AddToDashboardMenu');
var Widget = require('../widgets/Widget');

var FieldQuickValuesStore = require('../../stores/field-analyzers/FieldQuickValuesStore');

var FieldQuickValues = React.createClass({
    getInitialState() {
        return {
            field: undefined,
            data: []
        };
    },
    addFieldQuickValues(field) {
        this.setState({field: field}, this._loadQuickValuesData);
    },
    _loadQuickValuesData() {
        var promise = FieldQuickValuesStore.getQuickValues(this.state.field);
        promise.done((data) => this.setState({data: data}));
    },
    _resetStatus() {
        this.setState(this.getInitialState());
    },
    render() {
        var content;

        if (this.state.field !== undefined) {
            content = (
                <div className="content-col">
                    <div className="pull-right">
                        <AddToDashboardMenu title='Add to dashboard'
                                            dashboards={this.props.dashboards}
                                            widgetType={Widget.Type.QUICKVALUES}
                                            configuration={{field: this.state.field}}
                                            bsStyle='default'
                                            pullRight={true}>
                            <Button bsSize='small' onClick={() => this._resetStatus()}>Dismiss</Button>
                        </AddToDashboardMenu>
                    </div>
                    <h1>Field Statistics</h1>

                    <div>
                        <QuickValuesVisualization id={this.state.field}
                                                  horizontal={true}
                                                  config={{show_pie_chart: true, show_data_table: true}}
                                                  data={this.state.data}/>
                    </div>
                </div>
            );
        }

        return <div id="field-quick-values">{content}</div>;
    }
});

module.exports = FieldQuickValues;