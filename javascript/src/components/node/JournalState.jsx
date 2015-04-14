'use strict';

var React = require('react');
//noinspection JSUnusedGlobalSymbols
var MetricsStore = require('../../stores/metrics/MetricsStore');
var numeral = require('numeral');

var metricsStore = MetricsStore.instance;

var JournalState = React.createClass({
    getInitialState() {
        return ({
            initialized: false,
            hasError: false,
            append: 0,
            read: 0,
            segments: 0,
            entriesUncommitted: 0
        });
    },

    componentWillMount() {
        var metricNames = [
            "org.graylog2.journal.append.1-sec-rate",
            "org.graylog2.journal.read.1-sec-rate",
            "org.graylog2.journal.segments",
            "org.graylog2.journal.entries-uncommitted"
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

                var newState = {
                    initialized: true,
                    hasError: hasError
                };
                // we will only get one result, because we ask for only one node
                // get the base name from the metric name, and put the gauge metrics into our new state.
                update[0].values.forEach((namedMetric) => {
                    var baseName = namedMetric.name.replace('org.graylog2.journal.', '').replace('.1-sec-rate', '');
                    var camelCase = this.camelCase(baseName);
                    newState[camelCase] = namedMetric.metric.value;
                });
                this.setState(newState);
            }
        });
    },

    render() {
        return (<span>
            <strong>{numeral(this.state.entriesUncommitted).format('0,0')} unprocessed messages</strong> are currently in the journal, in {this.state.segments} segments. <strong>
            {numeral(this.state.append).format('0,0')} messages</strong> have been appended to, and <strong>
            {numeral(this.state.read).format('0,0')} messages</strong> have been read from the journal in the last second.
            </span>
        );
    },
    camelCase(input) {
        return input.toLowerCase().replace(/-(.)/g, function(match, group1) {
            return group1.toUpperCase();
        });
    }
});

module.exports = JournalState;

