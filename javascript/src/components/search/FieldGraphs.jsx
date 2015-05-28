'use strict';

var React = require('react');
var Immutable = require('immutable');

var LegacyFieldGraph = require('./LegacyFieldGraph');
var FieldGraphsStore = require('../../stores/field-analyzers/FieldGraphsStore');
var UIUtils = require('../../util/UIUtils');

var FieldGraphs = React.createClass({
    getInitialState() {
        this.notifyOnNewGraphs = false;
        this.newGraphs = Immutable.Set();

        return {
            fieldGraphs: FieldGraphsStore.fieldGraphs
        };
    },
    componentDidMount() {
        this.initialFieldGraphs = this.state.fieldGraphs;
        this.notifyOnNewGraphs = true;

        FieldGraphsStore.onFieldGraphsUpdated = (newFieldGraphs) => this.setState({fieldGraphs: newFieldGraphs});
        FieldGraphsStore.onFieldGraphCreated = (graphId) => {
            if (this.notifyOnNewGraphs && !this.initialFieldGraphs.has(graphId)) {
                var element = React.findDOMNode(this.refs[graphId]);
                UIUtils.scrollToHint(element);
            }
        };
    },
    _afterInitialGraphsLoaded() {
        this.notifyOnNewGraphs = true;
    },
    addFieldGraph(field) {
        FieldGraphsStore.newFieldGraph(field, {interval: this.props.resolution});
    },
    deleteFieldGraph(graphId) {
        FieldGraphsStore.deleteGraph(graphId);
    },
    render() {
        var fieldGraphs = [];

        this.state.fieldGraphs
            .sortBy(graph => graph['createdAt'])
            .forEach((graphOptions, graphId) => {
                fieldGraphs.push(
                    <LegacyFieldGraph key={graphId}
                                      ref={graphId}
                                      graphId={graphId}
                                      graphOptions={graphOptions}
                                      onDelete={() => this.deleteFieldGraph(graphId)}
                                      from={this.props.from}
                                      to={this.props.to}
                                      permissions={this.props.permissions}/>
                );
            });

        return (
            <div id="field-graphs">
                {fieldGraphs}
            </div>
        );
    }
});

module.exports = FieldGraphs;