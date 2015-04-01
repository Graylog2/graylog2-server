'use strict';

var React = require('react');
//noinspection JSUnusedGlobalSymbols
var MetricsStore = require('../../stores/metrics/MetricsStore');

var metricsStore = MetricsStore.instance;

var Throughput = React.createClass({
    getInitialState() {
        return {
            nodeCount: 0,
            totalIn: 0,
            totalOut: 0,
            hasError: false
        };
    },
    componentDidMount() {
        metricsStore.listen({
            nodeId: null, // across all nodes
            metricNames: ["org.graylog2.throughput.input.1-sec-rate", "org.graylog2.throughput.output.1-sec-rate"],
            callback: (update, hasError) => {
                // update is [{nodeId, values: [{name, value: {metric}}]} ...]
                // metric can be various different things, depending on metric {type: "GAUGE"|"COUNTER"|"METER"|"TIMER"}
                var nodeCount = update.length;

                var throughIn = 0;
                var throughOut = 0;
                // not using filter.map.reduce because that's even worse to read than this code...
                update.forEach((perNode) => {
                    perNode.values.forEach((namedMetric) => {
                        if (namedMetric.name === "org.graylog2.throughput.input.1-sec-rate") {
                            throughIn += namedMetric.metric.value;
                        } else if (namedMetric.name === "org.graylog2.throughput.output.1-sec-rate") {
                            throughOut += namedMetric.metric.value;
                        }
                    });
                });
                this.setState({nodeCount: nodeCount, totalIn: throughIn, totalOut: throughOut, hasError: hasError});
            }
        });
    },

    render() {
        if (this.state.hasError) {
            return (
                <span>
                    <strong className="total-throughput">Throughput unavailable</strong>
                </span>
            );
        }

        var nodeMarkup;
        if (this.state.nodeCount > 1) {
            nodeMarkup = <span className="total-nodes">across {this.state.nodeCount} nodes</span>;
        } else {
            nodeMarkup = <span className="total-nodes">on 1 node</span>;
        }
        if (this.state.nodeCount === 0) {
            return (
                <span>
                    <strong className="total-throughput">Loading throughput...</strong>
                </span>
            );
        }
        // TODO use numeral for formatting large numbers
        return (
            <span>
                In <strong className="total-throughput">{this.state.totalIn}</strong> / Out <strong className="total-throughput">{this.state.totalOut}</strong> msg/s {nodeMarkup}
            </span>

        );
    }
});

module.exports = Throughput;
