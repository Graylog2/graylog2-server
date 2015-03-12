/* global assertUpdateEnabled */
'use strict';

var React = require('react');

var Qs = require('qs');
var URLUtils = require("../../util/URLUtils");

var WidgetHeader = require('./WidgetHeader');
var WidgetFooter = require('./WidgetFooter');

var BootstrapModal = require('../bootstrap/BootstrapModal');

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
            boundToStream: false,
            config: {},
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
                creatorUserId: widget.creator_user_id,
                config: widget.config,
                boundToStream: "stream_id" in widget.config
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
        if (this.state.boundToStream) {
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
    _showConfig() {
        this.refs.configModal.open();
    },
    _hideConfig() {
        this.refs.configModal.close();
    },
    _goToWidgetMetrics() {
        window.location = this.metricsUrl();
    },
    _getBasicConfiguration() {
        var basicConfigurationMessage;
        if (this.state.boundToStream) {
            basicConfigurationMessage = (
                <p>
                    Type: {this.state.type.toLowerCase()}, cached for {this.state.cacheTime} seconds.&nbsp;
                    Widget is bound to stream {this.state.config.stream_id}.
                </p>
            );
        } else {
            basicConfigurationMessage = (
                <p>
                    Type: {this.state.type.toLowerCase()}, cached for {this.state.cacheTime} seconds.&nbsp;
                    Widget is <strong>not</strong> bound to a stream.
                </p>
            );
        }

        return basicConfigurationMessage;
    },
    _formatConfigurationKey(key) {
        return key.replace(/_/g, " ");
    },
    _formatConfigurationValue(key, value) {
        return key === "query" && value === "" ? "*" : String(value);
    },
    _getConfigAsDescriptionList() {
        var configKeys = Object.keys(this.state.config);
        if (configKeys.length === 0) {
            return [];
        }
        var configListElements = [];

        configKeys.forEach((key) => {
            configListElements.push(<dt key={key}>{this._formatConfigurationKey(key)}:</dt>);
            configListElements.push(<dd key={key + "-value"}>{this._formatConfigurationValue(key, this.state.config[key])}</dd>);
        });

        return configListElements;
    },
    render() {
        var configModalHeader = <h2>Widget "{this.state.title}" configuration</h2>;
        var configModalBody = (
            <div className="configuration">
                {this._getBasicConfiguration()}
                <div>More details:
                    <dl className="dl-horizontal">
                        <dt>Widget ID:</dt>
                        <dd>{this.props.widgetId}</dd>
                        <dt>Dashboard ID:</dt>
                        <dd>{this.props.dashboardId}</dd>
                        <dt>Created by:</dt>
                        <dd>{this.state.creatorUserId}</dd>
                        {this._getConfigAsDescriptionList()}
                    </dl>
                </div>
            </div>
        );

        return (
            <div className="widget">
                <WidgetHeader title={this.state.title} calculatedAt={this.state.calculatedAt}/>

                {this.getVisualization()}

                <WidgetFooter onReplaySearch={this._replaySearch} onShowConfig={this._showConfig}/>

                <BootstrapModal ref="configModal"
                                onCancel={this._hideConfig}
                                onConfirm={this._goToWidgetMetrics}
                                cancel="Cancel"
                                confirm="Show widget metrics">
                   {configModalHeader}
                   {configModalBody}
                </BootstrapModal>
            </div>
        );
    }
});

module.exports = Widget;