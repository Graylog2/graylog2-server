/* global assertUpdateEnabled, dashboardGrid */
'use strict';

var React = require('react');

var $ = require("jquery");

var Qs = require('qs');
var URLUtils = require("../../util/URLUtils");

var UserNotification = require("../../util/UserNotification");

var WidgetHeader = require('./WidgetHeader');
var WidgetFooter = require('./WidgetFooter');
var WidgetConfigModal = require('./WidgetConfigModal');
var WidgetEditConfigModal = require('./WidgetEditConfigModal');

var NumericVisualization = require('../visualizations/NumericVisualization');
var HistogramVisualization = require('../visualizations/HistogramVisualization');
var QuickValuesVisualization = require('../visualizations/QuickValuesVisualization');
var GraphVisualization = require('../visualizations/GraphVisualization');

var WidgetsStore = require('../../stores/widgets/WidgetsStore');

var Widget = React.createClass({
    WIDGET_DATA_REFRESH: 30 * 1000,
    DEFAULT_WIDGET_VALUE_REFRESH: 10 * 1000,
    WIDGET_FOOTER_HEIGHT: 25,
    statics: {
        Type: {
            SEARCH_RESULT_COUNT: "SEARCH_RESULT_COUNT",
            STREAM_SEARCH_RESULT_COUNT: "STREAM_SEARCH_RESULT_COUNT",
            STATS_COUNT: "STATS_COUNT",
            SEARCH_RESULT_CHART: "SEARCH_RESULT_CHART",
            QUICKVALUES: "QUICKVALUES",
            FIELD_CHART: "FIELD_CHART"
        }
    },

    getInitialState() {
        return {
            deleted: false,
            locked: true,
            type: "",
            title: "",
            cacheTime: 10,
            creatorUserId: "",
            config: {},
            result: undefined,
            calculatedAt: undefined,
            error: false,
            errorMessage: undefined,
            height: undefined,
            width: undefined
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
    _getWidgetNode() {
        return React.findDOMNode(this.refs.widget);
    },
    componentDidMount() {
        this._calculateWidgetSize();
        this._loadData();
        this._loadValue();

        $(this._getWidgetNode()).on("unlocked.graylog.dashboard", this._dashboardUnlocked);
        $(this._getWidgetNode()).on("locked.graylog.dashboard", this._dashboardLocked);
        $(document).on('gridster:resizestop', () => this._calculateWidgetSize());
    },
    componentWillUnmount() {
        $(this._getWidgetNode()).off("unlocked.graylog.dashboard", this._dashboardUnlocked);
        $(this._getWidgetNode()).off("locked.graylog.dashboard", this._dashboardLocked);
        $(document).off('gridster:resizestop', () => this._calculateWidgetSize());
    },
    _loadData() {
        if (!assertUpdateEnabled(this._loadData) || this.state.deleted) {
            return;
        }

        var widgetPromise = WidgetsStore.loadWidget(this.props.dashboardId, this.props.widgetId);
        widgetPromise.fail((jqXHR, textStatus, errorThrown) => {
            if (jqXHR.status === 404) {
                UserNotification.warning("It looks like widget '" + this.state.title + "' does not exist anymore. " +
                    "Please refresh the page to remove it from the dashboard.",
                    "Could not load widget information");
                this.setState({deleted: true});
            }
        });
        widgetPromise.done((widget) => {
            this.setState({
                type: widget.type,
                title: widget.title,
                cacheTime: widget.cacheTime,
                creatorUserId: widget.creatorUserId,
                config: widget.config
            });
        });
        setTimeout(this._loadData, this.WIDGET_DATA_REFRESH);
    },
    _loadValue() {
        if (!assertUpdateEnabled(this._loadValue) || this.state.deleted) {
            return;
        }

        var width = this.refs.widget.getDOMNode().clientWidth;

        var dataPromise = WidgetsStore.loadValue(this.props.dashboardId, this.props.widgetId, width);
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

        setTimeout(this._loadValue, Math.min(this.state.cacheTime * 1000, this.DEFAULT_WIDGET_VALUE_REFRESH));
    },
    _dashboardLocked() {
        this.setState({locked: true});
    },
    _dashboardUnlocked() {
        this.setState({locked: false});
    },
    _calculateWidgetSize() {
        var $widgetNode = $(this._getWidgetNode());
        var availableHeight = $widgetNode.height() - this.WIDGET_FOOTER_HEIGHT;
        var availableWidth = $widgetNode.width();
        this.setState({height: availableHeight, width: availableWidth});
    },
    _getVisualization() {
        if (this.state.type === "") {
            return;
        }

        var visualization;

        switch (this.state.type) {
            case this.constructor.Type.SEARCH_RESULT_COUNT:
            case this.constructor.Type.STREAM_SEARCH_RESULT_COUNT:
            case this.constructor.Type.STATS_COUNT:
                visualization = <NumericVisualization data={this.state.result} config={this.state.config}/>;
                break;
            case this.constructor.Type.SEARCH_RESULT_CHART:
                visualization = <HistogramVisualization id={this.props.widgetId}
                                                        data={this.state.result}
                                                        interval={this.state.config.interval}
                                                        height={this.state.height}
                                                        width={this.state.width}/>;
                break;
            case this.constructor.Type.QUICKVALUES:
                // We need to correct the size due to overflow on this DOM element
                var quickValuesWidth = this.state.width * 0.85;
                var quickValuesHeight = this.state.height * 0.85;
                visualization = <QuickValuesVisualization id={this.props.widgetId}
                                                          config={this.state.config}
                                                          data={this.state.result}
                                                          height={quickValuesHeight}
                                                          width={quickValuesWidth}/>;
                break;
            case this.constructor.Type.FIELD_CHART:
                visualization = <GraphVisualization id={this.props.widgetId}
                                                    data={this.state.result}
                                                    config={this.state.config}
                                                    height={this.state.height}
                                                    width={this.state.width}/>;
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
    _replaySearch(e) {
        URLUtils.openLink(this.replayUrl(), e.metaKey || e.ctrlKey);
    },
    _goToWidgetMetrics() {
        URLUtils.openLink(this.metricsUrl(), false);
    },
    _showConfig() {
        this.refs.configModal.open();
    },
    _showEditConfig() {
        this.refs.editModal.open();

        // Ugly workaround to avoid being able to move a widget when the modal is shown :(
        this._disableGridster();
    },
    _disableGridster() {
        dashboardGrid.disable();
    },
    _enableGridster() {
        dashboardGrid.enable();
    },
    updateWidget(newWidgetData) {
        newWidgetData.id = this.props.widgetId;

        WidgetsStore.updateWidget(this.props.dashboardId, newWidgetData);
        this.setState({
            title: newWidgetData.title,
            cacheTime: newWidgetData.cacheTime,
            config: newWidgetData.config
        });
    },
    deleteWidget() {
        if (window.confirm("Do you really want to delete '" + this.state.title + "'?")) {
            this.setState({deleted: true});
            $(".dashboard").trigger("delete.graylog.widget", {widgetId: this.props.widgetId});
        }
    },
    render() {
        var showConfigModal = (
            <WidgetConfigModal ref="configModal"
                               widget={this._getWidgetData()}
                               boundToStream={this._isBoundToStream()}
                               metricsAction={this._goToWidgetMetrics}/>
        );

        var editConfigModal = (
            <WidgetEditConfigModal ref="editModal"
                                   widget={this._getWidgetData()}
                                   onUpdate={this.updateWidget}
                                   onModalHidden={this._enableGridster}/>
        );

        return (
            <div ref="widget" className="widget" data-widget-id={this.props.widgetId}>
                <WidgetHeader title={this.state.title}
                              calculatedAt={this.state.calculatedAt}
                              error={this.state.error}
                              errorMessage={this.state.errorMessage}/>

                {this._getVisualization()}

                <WidgetFooter locked={this.state.locked}
                              onReplaySearch={this._replaySearch}
                              onShowConfig={this._showConfig}
                              onEditConfig={this._showEditConfig}
                              onDelete={this.deleteWidget}/>
                {this.state.locked ? showConfigModal : editConfigModal}
            </div>
        );
    }
});

module.exports = Widget;