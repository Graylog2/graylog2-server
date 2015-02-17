/* global assertUpdateEnabled */

'use strict';

var React = require('react');

var WidgetsStore = require('../../stores/widgets/WidgetsStore');

var BaseWidget = React.createClass({
    LOAD_WIDGET_DATA_INTERVAL: 30 * 1000,

    getInitialState() {
        return {
            description: "",
            cacheTime: 10
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
                cacheTime: widget.cache_time
            });
        });
        setTimeout(this.loadData, this.LOAD_WIDGET_DATA_INTERVAL);
    },
    loadValue() {
        if (!assertUpdateEnabled(this.loadValue)) { return; }

        this.props.loadValueCallback();
        setTimeout(this.loadValue, this.state.cacheTime * 1000);
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
            </div>
        );
        return widget;
    }
});

module.exports = BaseWidget;