'use strict';

var React = require('react');
var Immutable = require('immutable');
var Button = require('react-bootstrap').Button;

var AddToDashboardMenu = require('../dashboard/AddToDashboardMenu');
var Widget = require('../widgets/Widget');

var FieldStatisticsStore = require('../../stores/field-analyzers/FieldStatisticsStore');
var NumberUtils = require('../../util/NumberUtils');

var FieldStatistics = React.createClass({
    getInitialState() {
        return {
            fieldStatistics: Immutable.Map(),
            sortBy: 'field',
            sortDescending: false
        };
    },
    addFieldStatistics(field) {
        var promise = FieldStatisticsStore.getFieldStatistics(field);
        promise.done((statistics) => {
            this.setState({fieldStatistics: this.state.fieldStatistics.set(field, statistics)});
        });
    },
    _changeSortOrder(column) {
        if (this.state.sortBy === column) {
            this.setState({sortDescending: !this.state.sortDescending});
        } else {
            this.setState({sortBy: column, sortDescending: false});
        }
    },
    _resetStatus() {
        this.setState(this.getInitialState());
    },
    _renderStatistics() {
        var statistics = [];

        this.state.fieldStatistics.keySeq()
            .sort((key1, key2) => {
                var a = this.state.sortDescending ? key2 : key1;
                var b = this.state.sortDescending ? key1 : key2;

                if (this.state.sortBy === 'field') {
                    return a.toLowerCase().localeCompare(b.toLowerCase());
                }
                var statA = this.state.fieldStatistics.get(a)[this.state.sortBy];
                var statB = this.state.fieldStatistics.get(b)[this.state.sortBy];
                return NumberUtils.normalizeNumber(statA) - NumberUtils.normalizeNumber(statB);
            })
            .forEach((field) => {
                var stats = this.state.fieldStatistics.get(field);
                statistics.push(
                    <tr key={field}>
                        <td>{field}</td>
                        {FieldStatisticsStore.FUNCTIONS.keySeq().map((statFunction) => {
                            return <td key={statFunction + "-td"}>{NumberUtils.formatNumber(stats[statFunction])}</td>;
                        })}
                    </tr>
                );
            });

        return statistics;
    },
    _renderStatisticalFunctionsHeaders() {
        return FieldStatisticsStore.FUNCTIONS.keySeq().map((statFunction) => {
            return (
                <th key={statFunction + "-th"} onClick={() => this._changeSortOrder(statFunction)}>
                    {FieldStatisticsStore.FUNCTIONS.get(statFunction)} {this._getHeaderCaret(statFunction)}
                </th>
            );
        });
    },
    _getHeaderCaret(column) {
        if (this.state.sortBy !== column) {
            return;
        }
        return this.state.sortDescending ? <i className='fa fa-caret-down'></i> : <i className='fa fa-caret-up'></i>;
    },
    render() {
        var content;

        if (!this.state.fieldStatistics.isEmpty()) {
            content = (
                <div className="content-col">
                    <div className="pull-right">
                        <AddToDashboardMenu title='Add to dashboard'
                                            dashboards={this.props.dashboards}
                                            widgetType={Widget.Type.STATS_COUNT}
                                            configuration={{}}
                                            bsStyle='default'
                                            fields={this.state.fieldStatistics.keySeq()}
                                            pullRight={true}>
                            <Button bsSize='small' onClick={() => this._resetStatus()}>Dismiss</Button>
                        </AddToDashboardMenu>
                    </div>
                    <h1>Field Statistics</h1>

                    <div className='table-responsive'>
                        <table className='table table-striped table-bordered table-hover table-condensed'>
                            <thead>
                            <tr>
                                <th onClick={() => this._changeSortOrder('field')}>
                                    Field {this._getHeaderCaret('field')}
                                </th>
                                {this._renderStatisticalFunctionsHeaders()}
                            </tr>
                            </thead>
                            <tbody>
                            {this._renderStatistics()}
                            </tbody>
                        </table>
                    </div>
                </div>
            );
        }

        return (
            <div id='field-statistics'>
                {content}
            </div>
        );
    }
});

module.exports = FieldStatistics;