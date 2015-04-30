'use strict';

var React = require('react');
//noinspection JSUnusedGlobalSymbols
var MetricsStore = require('../../stores/metrics/MetricsStore');
var numeral = require('numeral');

var metricsStore = MetricsStore.instance;

var InputIOMetrics = React.createClass({
    getInitialState() {
        return {
            initialized: false,
            hasError: false,

            incomingMessages: 0,

            hasConnectionMetrics: false,
            open_connections: 0,
            total_connections: 0,

            hasNetworkMetrics: false,
            written_bytes_1sec: 0,
            written_bytes_total: 0,
            read_bytes_1sec: 0,
            read_bytes_total: 0
        };
    },

    _prefix: function (metric) {
        return this.props.metricsPrefix + '.' + this.props.inputId + '.' + metric;
    },

    componentWillMount() {
        var metricNames = [
            this._prefix('incomingMessages'),
            this._prefix('open_connections'),
            this._prefix('total_connections'),
            this._prefix('written_bytes_1sec'),
            this._prefix('written_bytes_total'),
            this._prefix('read_bytes_1sec'),
            this._prefix('read_bytes_total')
        ];

        metricsStore.listen({
            nodeId: this.props.nodeId, // might be null for global inputs
            metricNames: metricNames,
            callback: (update, hasError) => {
                if (hasError) {
                    this.setState({hasError: hasError});
                    return;
                }

                var newState = {
                    hasError: false,
                    initialized: true,
                    incomingMessages: 0,
                    open_connections: 0,
                    total_connections: 0,
                    written_bytes_1sec: 0,
                    written_bytes_total: 0,
                    read_bytes_1sec: 0,
                    read_bytes_total: 0
                };

                if (this.props.nodeId) {
                    // input on a single node, don't need to aggregate the updated metrics
                    update[0].values.forEach((namedMetric) => {
                        this._processNodeUpdate(namedMetric, newState);
                    });
                } else {
                    // this is a global input, we need to aggregate the values for all nodes that are being returned
                    update.forEach((perNode) => {
                        perNode.values.forEach((namedMetric) => {
                            this._processNodeUpdate(namedMetric, newState);
                        });
                    });
                }
                this.setState(newState);
            }
        });
    },

    _processNodeUpdate(namedMetric, newState) {
        var baseName = namedMetric.name.replace(this._prefix(''), '');
        var isKnownMetric = true;
        var value = 0;
        if (baseName === "incomingMessages") { // this a meter
            value = namedMetric.metric.one_minute;
        } else if (
            baseName === "open_connections" ||
            baseName === "total_connections") { // these are all gauges

            newState['hasConnectionMetrics'] = true;
            value = namedMetric.metric.value;
        } else if (baseName === "written_bytes_1sec" ||
            baseName === "written_bytes_total" ||
            baseName === "read_bytes_1sec" ||
            baseName === "read_bytes_total") { // these are all gauges

            newState['hasNetworkMetrics'] = true;
            value = namedMetric.metric.value;
        } else {
            isKnownMetric = false;
        }
        if (isKnownMetric) {
            newState[baseName] += value;
        }
    },

    render() {
        if (this.state.hasError) {
            return (<span>Input metrics unavailable</span>);
        }

        if (!this.state.initialized) {
            return (<span><i className="fa fa-spin fa-spinner"></i> Loading metrics...</span>);
        }

        var network = null;
        if (this.state.hasNetworkMetrics) {
            // ugh, is there a way of not doing it globally?
            numeral.zeroFormat('0B');
            network = (
                <span className="input-io">
                    <span>Network IO: </span>
                    <span className="persec">
                        <i className="fa fa-caret-down channel-direction channel-direction-down"></i>
                        <span className="rx value">{numeral(this.state.read_bytes_1sec).format("0.0b")} </span>

                        <i className="fa fa-caret-up channel-direction channel-direction-up"></i>
                        <span className="tx value">{numeral(this.state.written_bytes_1sec).format("0.0b")}</span>
                    </span>

                    <span className="total">
                        <span> (total: </span>
                        <i className="fa fa-caret-down channel-direction channel-direction-down"></i>
                        <span className="rx value">{numeral(this.state.read_bytes_total).format("0.0b")} </span>

                        <i className="fa fa-caret-up channel-direction channel-direction-up"></i>
                        <span className="tx value">{numeral(this.state.written_bytes_total).format("0.0b")}</span>
                        <span> )</span>
                    </span>
                </span>
            );
            // wow this sucks
            numeral.zeroFormat(null);
        }

        var connections = null;
        if (this.state.hasConnectionMetrics) {
            connections = (
                <span>Active connections: <span className="active">{numeral(this.state.open_connections).format("0,0")} </span>
                    (<span className="total">{numeral(this.state.open_connections).format("0,0")}</span> total)
                </span>
            );
        }

        var messages = (<span>
            1 minute average rate: {numeral(this.state.incomingMessages).format('0,0')} msg/s
        </span>);

        return (<span>
            {messages}
            {network ? <br/> : null}
            {network}
            {connections ? <br/> : null}
            {connections}
        </span>);
    }
});

module.exports = InputIOMetrics;