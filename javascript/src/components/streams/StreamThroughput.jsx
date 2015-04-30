'use strict';

var React = require('react');
//noinspection JSUnusedGlobalSymbols
var MetricsStore = require('../../stores/metrics/MetricsStore');

var metricsStore = MetricsStore.instance;

var StreamThroughput = React.createClass({
    getInitialState() {
        return ({
            initialized: false,
            throughput: 0,
            hasError: false
        });
    },
    componentWillMount() {
        var metricName = "org.graylog2.plugin.streams.Stream." + this.props.streamId + ".incomingMessages.1-sec-rate";
        metricsStore.listen({
            nodeId: null, // across all nodes
            metricNames: [metricName],
            callback: (update, hasError) => {
                // update is [{nodeId, values: [{name, value: {metric}}]} ...]
                // metric can be various different things, depending on metric {type: "GAUGE"|"COUNTER"|"METER"|"TIMER"}
                if (hasError) {
                    this.setState({hasError: hasError});
                    return;
                }

                var throughput = 0;
                // not using filter.map.reduce because that's even worse to read than this code...
                update.forEach((perNode) => {
                    perNode.values.forEach((namedMetric) => {
                        if (namedMetric.name === metricName) {
                            throughput += namedMetric.metric.value;
                        }
                    });
                });
                this.setState({initialized: true, throughput: throughput, hasError: hasError});
            }
        });
    },
    render() {
        if (this.state.hasError) {
            return (<span>Throughput unavailable</span>);
        }
        if (!this.state.initialized) {
            return (<span><i className="fa fa-spin fa-spinner"></i> Loading</span>);
        }
        return (
            <span>{this.state.throughput} messages/second</span>
        );

    }
});

module.exports = StreamThroughput;
