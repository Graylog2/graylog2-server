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
            statsLoadPending: Immutable.Map(),
            fieldStatistics: Immutable.Map(),
            sortBy: 'field',
            sortDescending: false,
            autoReload: true,
            updateIntervalId: null
        };
    },
    componentDidMount() {
        this.setState({updateIntervalId: window.setInterval(() => this._reloadAllStatistics(), 3000)});
    },
    componentWillUnmount() {
        window.clearInterval(this.state.updateIntervalId);
    },
    addFieldStatistics(field) {
        this._reloadFieldStatistics(field);
    },
    _reloadAllStatistics() {
        if (this.state.autoReload) {
            this.state.fieldStatistics.keySeq().forEach((field) => this._reloadFieldStatistics(field));
        }
    },
    _reloadFieldStatistics(field) {
        if (this.isMounted) {
            this.setState({statsLoadPending: this.state.statsLoadPending.set(field, true)});
            var promise = FieldStatisticsStore.getFieldStatistics(field);
            promise.done((statistics) => {
                this.setState({
                    fieldStatistics: this.state.fieldStatistics.set(field, statistics),
                    statsLoadPending: this.state.statsLoadPending.set(field, false)
                });
            }).fail((jqXHR) => {
                // if the field has no statistics to display, remove it from the set of fields (which will cause the component to not render)
                if (jqXHR.status === 400) {
                    this.setState({
                        fieldStatistics: this.state.fieldStatistics.delete(field),
                        statsLoadPending: this.state.statsLoadPending.delete(field)
                    });
                }
            });
        }
    },
    _changeSortOrder(column) {
        if (this.state.sortBy === column) {
            this.setState({sortDescending: !this.state.sortDescending});
        } else {
            this.setState({sortBy: column, sortDescending: false});
        }
    },
    _toggleAutoReload() {
        var shouldAutoReload = !this.state.autoReload;
        this.setState({autoReload: shouldAutoReload});
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
                var maybeSpinner = null;
                if (this.state.statsLoadPending.get(field)) {
                    maybeSpinner = <i className="fa fa-spin fa-spinner"></i>;
                }
                statistics.push(
                    <tr key={field}>
                        <td>{maybeSpinner}</td>
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
                                            widgetType={Widget.Type.STATS_COUNT}
                                            configuration={{}}
                                            bsStyle='default'
                                            fields={this.state.fieldStatistics.keySeq()}
                                            pullRight={true}
                                            permissions={this.props.permissions}>

                            <Button bsSize='small' onClick={() => this._toggleAutoReload()}>{this.state.autoReload ? "Stop reloading" : "Reload automatically"} </Button>
                            <Button bsSize='small' onClick={() => this._resetStatus()}>Dismiss</Button>
                        </AddToDashboardMenu>
                    </div>
                    <h1>Field Statistics</h1>

                    <div className='table-responsive'>
                        <table className='table table-striped table-bordered table-hover table-condensed'>
                            <thead>
                            <tr>
                                <th style={{width: 24}}></th>
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
        } else if(!this.state.statsLoadPending.isEmpty()) {
            content = (<div className="content-col">
                <h1>Field Statistics <i className="fa fa-spin fa-spinner"></i></h1>
            </div>);
        }

        return (
            <div id='field-statistics'>
                {content}
            </div>
        );
    }
});

module.exports = FieldStatistics;