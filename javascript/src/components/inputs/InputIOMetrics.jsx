'use strict';

var React = require('react');
//noinspection JSUnusedGlobalSymbols
var MetricsStore = require('../../stores/metrics/MetricsStore');
var NodesStore = require('../../stores/nodes/NodesStore');
var Immutable = require('immutable');
var numeral = require('numeral');

var metricsStore = MetricsStore.instance;
var nodesStore = NodesStore.instance;

var InputIOMetrics = React.createClass({
    getInitialState() {
        return {
            initialized: false,
            hasError: false,
            showDetails: false,
            global: this._newMetricState(),
            nodes: Immutable.Map()
        };
    },

    _prefix: function (metric) {
        return this.props.metricsPrefix + '.' + this.props.inputId + '.' + metric;
    },

    _newMetricState: function () {
        return {
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

        // We currently only need the NodeStore if we are rendering a global input.
        if (this._isGlobalInput()) {
            nodesStore.load();
        }

        metricsStore.listen({
            nodeId: this.props.nodeId, // might be null for global inputs
            metricNames: metricNames,
            callback: (update, hasError) => {
                if (hasError) {
                    this.setState({hasError: hasError});
                    return;
                }

                var newState = this.getInitialState();
                newState.initialized = true;
                newState.showDetails = this.state.showDetails;

                if (!this._isGlobalInput()) {
                    // input on a single node, don't need to aggregate the updated metrics
                    update[0].values.forEach((namedMetric) => {
                        this._processNodeUpdate(namedMetric, newState.global);
                    });
                } else {
                    // this is a global input, we need to aggregate the values for all nodes that are being returned
                    update.forEach((perNode) => {
                        newState.nodes = newState.nodes.set(perNode.node_id, this._newMetricState());

                        perNode.values.forEach((namedMetric) => {
                            this._processNodeUpdate(namedMetric, newState.global);
                            this._processNodeUpdate(namedMetric, newState.nodes.get(perNode.node_id));
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

    _renderNetworkMetrics(nodeState) {
        if (nodeState === null) {
            return null;
        }

        // ugh, is there a way of not doing it globally?
        numeral.zeroFormat('0B');

        var network = (
            <span className="input-io">
                    <span>Network IO: </span>
                    <span className="persec">
                        <i className="fa fa-caret-down channel-direction channel-direction-down"></i>
                        <span className="rx value">{numeral(nodeState.read_bytes_1sec).format("0.0b")} </span>

                        <i className="fa fa-caret-up channel-direction channel-direction-up"></i>
                        <span className="tx value">{numeral(nodeState.written_bytes_1sec).format("0.0b")}</span>
                    </span>

                    <span className="total">
                        <span> (total: </span>
                        <i className="fa fa-caret-down channel-direction channel-direction-down"></i>
                        <span className="rx value">{numeral(nodeState.read_bytes_total).format("0.0b")} </span>

                        <i className="fa fa-caret-up channel-direction channel-direction-up"></i>
                        <span className="tx value">{numeral(nodeState.written_bytes_total).format("0.0b")}</span>
                        <span> )</span>
                    </span>
                </span>
        );
        // wow this sucks
        numeral.zeroFormat(null);

        return network;
    },

    _renderConnectionMetrics(nodeState) {
        if (nodeState === null) {
            return null;
        }

        return (
            <span>Active connections: <span className="active">{numeral(nodeState.open_connections).format("0,0")} </span>
                    (<span className="total">{numeral(nodeState.open_connections).format("0,0")}</span> total)
            </span>
        );
    },

    _renderNodesDetails(nodesState) {
        var nodes = [];

        if (nodesState === null) {
            return nodes;
        }

        nodes.push(<hr key={'separator'}/>);

        nodesState.forEach((state, nodeId) => {
            var nodeName = nodeId;
            var nodeDetails = nodesStore.get(nodeId);

            if (nodeDetails) {
                nodeName = nodeDetails.short_node_id + " / " + nodeDetails.hostname;
            } else {
                // Trigger load to eventually get the node details.
                nodesStore.load();
            }

            nodes.push(
                <span key={this.props.inputId + nodeId}>
                    <strong>{nodeName}</strong>
                    <br/>
                    {this._renderNetworkMetrics(state)}
                    <br/>
                </span>
            );
        });

        return nodes;
    },

    _toggleShowDetails(e) {
        e.preventDefault();

        if (this.state.showDetails) {
            this.setState({showDetails: false});
        } else {
            this.setState({showDetails: true});
        }
    },

    _isGlobalInput() {
        return !this.props.nodeId;
    },

    render() {
        if (this.state.hasError) {
            return (<span>Input metrics unavailable</span>);
        }

        if (!this.state.initialized) {
            return (<span><i className="fa fa-spin fa-spinner"></i> Loading metrics...</span>);
        }

        var network = null;
        if (this.state.global.hasNetworkMetrics) {
            network = this._renderNetworkMetrics(this.state.global);
        }

        var connections = null;
        if (this.state.global.hasConnectionMetrics) {
            connections = this._renderConnectionMetrics(this.state.global);
        }

        var messages = (<span>
            1 minute average rate: {numeral(this.state.global.incomingMessages).format('0,0')} msg/s
        </span>);

        var showDetailsLink = null;
        if (this._isGlobalInput()) {
            showDetailsLink = (<a href="" onClick={this._toggleShowDetails}>Show details</a>);
        }

        var nodes = [];
        if (this.state.showDetails) {
            nodes = this._renderNodesDetails(this.state.nodes);
        }

        return (<span>
            {messages}
            {network ? <br/> : null}
            {network}
            {' '}
            {showDetailsLink}
            {connections ? <br/> : null}
            {connections}
            {nodes}
        </span>);
    }
});

module.exports = InputIOMetrics;