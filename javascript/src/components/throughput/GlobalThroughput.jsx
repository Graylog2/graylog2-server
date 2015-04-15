'use strict';

var React = require('react');
//noinspection JSUnusedGlobalSymbols
var MetricsStore = require('../../stores/metrics/MetricsStore');
var numeral = require('numeral');
var metricsStore = MetricsStore.instance;

var GlobalThroughput = React.createClass({
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

        if (this.state.nodeCount === 0) {
            return (
                <span>
                    <strong className="total-throughput"><i className="fa fa-spin fa-spinner"></i> Loading throughput...</strong>
                </span>
            );
        }
        return (
            <span>
                In <strong className="total-throughput">{numeral(this.state.totalIn).format('0,0')}</strong> / Out <strong className="total-throughput">{numeral(this.state.totalOut).format('0,0')}</strong> msg/s
            </span>

        );
    }
});

module.exports = GlobalThroughput;
