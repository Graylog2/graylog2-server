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
                        <td>{NumberUtils.formatNumber(stats['count'])}</td>
                        <td>{NumberUtils.formatNumber(stats['mean'])}</td>
                        <td>{NumberUtils.formatNumber(stats['min'])}</td>
                        <td>{NumberUtils.formatNumber(stats['max'])}</td>
                        <td>{NumberUtils.formatNumber(stats['std_deviation'])}</td>
                        <td>{NumberUtils.formatNumber(stats['variance'])}</td>
                        <td>{NumberUtils.formatNumber(stats['sum'])}</td>
                    </tr>
                );
            });

        return statistics;
    },
    _getHeaderCaret(column) {
        if (this.state.sortBy !== column) {
            return;
        }
        return this.state.sortDescending ? <i className='fa fa-caret-down'></i> : <i className='fa fa-caret-up'></i>;
    },
    render() {
        return (
            <div id='field-statistics'>
                {!this.state.fieldStatistics.isEmpty() &&
                <div className="content-col">
                    <div className="pull-right">
                        <AddToDashboardMenu title='Add to dashboard'
                                            dashboards={this.props.dashboards}
                                            widgetType={Widget.Type.STATS_COUNT}
                                            configuration={{}}
                                            bsStyle='default'
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
                                <th onClick={() => this._changeSortOrder('count')}>
                                    Total {this._getHeaderCaret('count')}
                                </th>
                                <th onClick={() => this._changeSortOrder('mean')}>
                                    Mean {this._getHeaderCaret('mean')}
                                </th>
                                <th onClick={() => this._changeSortOrder('min')}>
                                    Minimum {this._getHeaderCaret('min')}
                                </th>
                                <th onClick={() => this._changeSortOrder('max')}>
                                    Maximum {this._getHeaderCaret('max')}
                                </th>
                                <th onClick={() => this._changeSortOrder('std_deviation')}>
                                    Std. deviation {this._getHeaderCaret('std_deviation')}
                                </th>
                                <th onClick={() => this._changeSortOrder('variance')}>
                                    Variance {this._getHeaderCaret('variance')}
                                </th>
                                <th onClick={() => this._changeSortOrder('sum')}>
                                    Sum {this._getHeaderCaret('sum')}
                                </th>
                            </tr>
                            </thead>
                            <tbody>
                            {this._renderStatistics()}
                            </tbody>
                        </table>
                    </div>
                </div>
                }
            </div>
        );
    }
});

module.exports = FieldStatistics;