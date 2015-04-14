/* global jsRoutes */

'use strict';

var React = require('react');
var ProgressBar = require('react-bootstrap').ProgressBar;
var Button = require('react-bootstrap').Button;
//noinspection JSUnusedGlobalSymbols
var MetricsStore = require('../../stores/metrics/MetricsStore');
var numeral = require('numeral');

var metricsStore = MetricsStore.instance;

var BufferUsage = React.createClass({
    getInitialState() {
        return ({
            usagePercentage: 0,
            usage: 0,
            size: 0,
            hasError: false,
            initialized: true
        });
    },
    componentWillMount() {
        var metricNames = [
            this._metricPrefix() + 'usage',
            this._metricPrefix() + 'size'
        ];
        metricsStore.listen({
            nodeId: this.props.nodeId,
            metricNames: metricNames,
            callback: (update, hasError) => {
                if (hasError) {
                    this.setState({hasError: hasError});
                }
                // update is [{nodeId, values: [{name, value: {metric}}]} ...]
                // metric can be various different things, depending on metric {type: "GAUGE"|"COUNTER"|"METER"|"TIMER"}

                var usage = 0;
                var size = 0;
                // we will only get one result, because we ask for only one node
                update[0].values.forEach((namedMetric) => {
                    if (namedMetric.name === this._metricPrefix() + 'usage') {
                        usage = namedMetric.metric.value;
                    }
                    if (namedMetric.name === this._metricPrefix() + 'size') {
                        size = namedMetric.metric.value;
                    }
                });
                var usagePercentage = size === 0 ? 0 : (usage / size) * 100;
                this.setState({
                    initialized: true,
                    usagePercentage: usagePercentage,
                    usage: usage,
                    size: size,
                    hasError: hasError
                });
            }
        });
    },
    render() {
        var percentLabel = numeral(this.state.usagePercentage / 100).format('0.0 %');
        return (
            <div>
                <Button href={jsRoutes.controllers.MetricsController.ofNode(this.props.nodeId, this._metricPrefix()).url} bsSize="xsmall" className="pull-right">Metrics</Button>
                <h3>{this.props.title}</h3>
                <div className="node-buffer-usage">
                    <ProgressBar now={this.state.usagePercentage}
                                 bsStyle='warning'
                                 label={percentLabel} />
                </div>
                <span><strong>{this.state.usage} messages</strong> in {this.props.title.toLowerCase()}, {percentLabel} utilized.</span>
            </div>);
    },
    _metricPrefix() {
        return 'org.graylog2.buffers.' + this.props.bufferType + '.';
    }

});

module.exports = BufferUsage;