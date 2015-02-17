/* global assertUpdateEnabled */

'use strict';

var React = require('react');

var URLUtils = require("../../util/URLUtils");
var Qs = require('qs');

var WidgetsStore = require('../../stores/widgets/WidgetsStore');

var BaseWidget = React.createClass({
    LOAD_WIDGET_DATA_INTERVAL: 30 * 1000,

    getInitialState() {
        return {
            description: "",
            cacheTime: 10,
            query: undefined,
            streamId: undefined,
            rangeType: "",
            range: 0,
            interval: undefined
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
                description: widget.description,
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

        this.props.loadValueCallback();
        setTimeout(this.loadValue, this.state.cacheTime * 1000);
    },
    _getUrlPath() {
        if (this.state.streamId === undefined) {
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
                <div className="widget-title">
                    {this.state.description}
                </div>
                <div className="widget-calculated-at" title={this.props.calculatedAt}>
                    {moment(this.props.calculatedAt).fromNow()}
                </div>

                {this.props.children}

                <div>
                    <div className="widget-info">
                        <a href="#">
                            <i className="icon icon-info-sign"></i>
                        </a>
                    </div>
                    <div className="widget-replay">
                        <a href={this.replayUrl()}>
                            <i className="icon icon-play-sign"></i>
                        </a>
                    </div>
                </div>
            </div>
        );
        return widget;
    }
});

module.exports = BaseWidget;