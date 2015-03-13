/* global assertUpdateEnabled */
'use strict';

var React = require('react');

var Qs = require('qs');
var URLUtils = require("../../util/URLUtils");

var WidgetHeader = require('./WidgetHeader');
var WidgetFooter = require('./WidgetFooter');
var WidgetConfigModal = require('./WidgetConfigModal');

var CountVisualization = require('../visualizations/CountVisualization');
var HistogramVisualization = require('../visualizations/HistogramVisualization');

var WidgetsStore = require('../../stores/widgets/WidgetsStore');

var Widget = React.createClass({
    LOAD_WIDGET_DATA_INTERVAL: 30 * 1000,

    getInitialState() {
        return {
            type: "",
            title: "",
            cacheTime: 10,
            creatorUserId: "",
            config: {},
            result: undefined,
            calculatedAt: undefined,
            error: false,
            errorMessage: undefined
        };
    },
    _isBoundToStream() {
        return ("stream_id" in this.state.config) && (this.state.config.stream_id !== null);
    },
    _getWidgetData() {
        return {
            widgetId: this.props.widgetId,
            dashboardId: this.props.dashboardId,
            title: this.state.title,
            type: this.state.type,
            cacheTime: this.state.cacheTime,
            creatorUserId: this.state.creatorUserId,
            config: this.state.config
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
                creatorUserId: widget.creator_user_id,
                config: widget.config
            });
        });
        setTimeout(this.loadData, this.LOAD_WIDGET_DATA_INTERVAL);
    },
    loadValue() {
        if (!assertUpdateEnabled(this.loadValue)) { return; }

        var dataPromise = WidgetsStore.loadValue(this.props.dashboardId, this.props.widgetId);
        dataPromise.fail((jqXHR, textStatus, errorThrown) => {
            this.setState({
                error: true,
                errorMessage: "Error loading widget value: " + errorThrown
            });
        });
        dataPromise.done((value) => {
            this.setState({
                result: value.result,
                calculatedAt: value.calculated_at,
                error: false,
                errorMessage: undefined
            });
        });

        setTimeout(this.loadValue, this.state.cacheTime * 1000);
    },
    getVisualization() {
        if (this.state.type === "") { return; }

        var visualization;

        switch(this.state.type) {
            case 'SEARCH_RESULT_COUNT':
            case 'STREAM_SEARCH_RESULT_COUNT':
            case 'STATS_COUNT':
                visualization = <CountVisualization data={this.state.result} config={this.state.config}/>;
                break;
            case 'SEARCH_RESULT_CHART':
                visualization = <HistogramVisualization id={this.props.widgetId}
                                                        data={this.state.result}
                                                        interval={this.state.config.interval}/>;
                break;
            default:
                throw("Error: Widget type '" + this.state.type + "' not supported");
        }

        return visualization;
    },
    _getUrlPath() {
        if (this._isBoundToStream()) {
            return "/streams/" + this.state.config.stream_id + "/messages";
        } else {
            return "/search";
        }
    },
    _getUrlQueryString() {
        var query = {
            q: this.state.config.query,
            rangetype: this.state.config.range_type,
            interval: this.state.config.interval
        };
        if (this.state.config.range_type !== "absolute") {
            query[this.state.config.range_type] = this.state.config.range;
        } else {
            query["from"] = this.state.config.from;
            query["to"] = this.state.config.to;
        }

        return Qs.stringify(query);
    },
    replayUrl() {
        var path = this._getUrlPath();
        var queryString = this._getUrlQueryString();

        return URLUtils.appPrefixed(path + "?" + queryString);
    },
    metricsUrl() {
        var url = "/system/metrics/master";
        var query = {
            prefilter: "org.graylog2.dashboards.widgets.*." + this.props.widgetId
        };

        return URLUtils.appPrefixed(url + "?" + Qs.stringify(query));
    },
    _replaySearch() {
        window.location = this.replayUrl();
    },
    _goToWidgetMetrics() {
        window.location = this.metricsUrl();
    },
    _showConfig() {
        this.refs.configModal.open();
    },
    render() {
        return (
            <div className="widget" data-widget-id={this.props.widgetId}>
                <WidgetHeader title={this.state.title}
                              calculatedAt={this.state.calculatedAt}
                              error={this.state.error}
                              errorMessage={this.state.errorMessage}/>

                {this.getVisualization()}

                <WidgetFooter onReplaySearch={this._replaySearch} onShowConfig={this._showConfig}/>
                <WidgetConfigModal ref="configModal"
                    {...this._getWidgetData()}
                    boundToStream={this._isBoundToStream()}
                    metricsAction={this._goToWidgetMetrics}/>
            </div>
        );
    }
});

module.exports = Widget;