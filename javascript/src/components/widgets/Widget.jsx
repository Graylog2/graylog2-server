/* global assertUpdateEnabled */
'use strict';

var React = require('react');

var Qs = require('qs');
var URLUtils = require("../../util/URLUtils");

var WidgetHeader = require('./WidgetHeader');
var WidgetFooter = require('./WidgetFooter');
var CountVisualization = require('../visualizations/CountVisualization');
var HistogramVisualization = require('../visualizations/HistogramVisualization');

var WidgetsStore = require('../../stores/widgets/WidgetsStore');

var Widget = React.createClass({
    LOAD_WIDGET_DATA_INTERVAL: 30 * 1000,

    getInitialState() {
        return {
            type: undefined,
            title: "",
            cacheTime: 10,
            query: undefined,
            streamId: undefined,
            rangeType: "",
            range: 0,
            interval: undefined,
            result: undefined,
            calculatedAt: undefined
        };
    },
    componentDidMount() {
        this.loadData();
        this.loadValue();
    },
    loadData() {
        if (!assertUpdateEnabled(this.loadData)) { return; }

        var widgetPromise = WidgetsStore.loadWidget(this.props.dashboardId, this.props.widgetId);
        widgetPromise.done((widget) => {
            this.setState({
                type: widget.type,
                title: widget.description,
                cacheTime: widget.cache_time,
                query: widget.query,
                streamId: widget.stream_id,
                rangeType: widget.range_type,
                range: widget.range,
                interval: widget.interval
            });
        });
        setTimeout(this.loadData, this.LOAD_WIDGET_DATA_INTERVAL);
    },
    loadValue() {
        if (!assertUpdateEnabled(this.loadValue)) { return; }

        var dataPromise = WidgetsStore.loadValue(this.props.dashboardId, this.props.widgetId);
        dataPromise.done((value) => {
            this.setState({
                result: value.result,
                calculatedAt: value.calculated_at
            });
        });

        setTimeout(this.loadValue, this.state.cacheTime * 1000);
    },
    getVisualization() {
        if (this.state.type === undefined) { return; }

        var visualization;

        switch(this.state.type) {
            case 'SEARCH_RESULT_COUNT':
            case 'STREAM_SEARCH_RESULT_COUNT':
            case 'STATS_COUNT':
                visualization = <CountVisualization data={this.state.result}/>;
                break;
            case 'SEARCH_RESULT_CHART':
                visualization = <HistogramVisualization id={this.props.widgetId}
                                                        data={this.state.result}
                                                        interval={this.state.interval}/>;
                break;
            default:
                throw("Error: Widget type '" + this.state.type + "' not supported");
        }

        return visualization;
    },
    _getUrlPath() {
        if (this.state.streamId === undefined || this.state.streamId === null) {
            return "/search";
        } else {
            return "/streams/" + this.state.streamId + "/messages";
        }
    },
    _getUrlQueryString() {
        var query = {
            q: this.state.query,
            rangetype: this.state.rangeType,
            interval: this.state.interval
        };
        query[this.state.rangeType] = this.state.range;

        return Qs.stringify(query);
    },
    replayUrl() {
        var path = this._getUrlPath();
        var queryString = this._getUrlQueryString();

        return URLUtils.appPrefixed(path + "?" + queryString);
    },
    render() {
        var widget = (
            <div className="widget">
                <WidgetHeader title={this.state.title} calculatedAt={this.state.calculatedAt}/>

                {this.getVisualization()}
            </div>
        );
        return widget;
    }
});

module.exports = Widget;