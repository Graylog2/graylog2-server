'use strict';

var React = require('react');
var Immutable = require('immutable');

var LegacyFieldGraph = require('./LegacyFieldGraph');
var FieldGraphsStore = require('../../stores/field-analyzers/FieldGraphsStore');
var UIUtils = require('../../util/UIUtils');

var FieldGraphs = React.createClass({
    getInitialState() {
        this.notifyOnNewGraphs = false;

        return {
            fieldGraphs: FieldGraphsStore.fieldGraphs,
            stackedGraphs: Immutable.Map()
        };
    },
    componentDidMount() {
        this.initialFieldGraphs = this.state.fieldGraphs;
        this.notifyOnNewGraphs = true;

        FieldGraphsStore.onFieldGraphsUpdated = (newFieldGraphs) => this.setState({fieldGraphs: newFieldGraphs});
        FieldGraphsStore.onFieldGraphsMerged = (targetGraphId, draggedGraphId) => {
            var newStackedGraphs = this.state.stackedGraphs;

            if (newStackedGraphs.has(targetGraphId)) {
                // targetGraphId was a stacked graph
                newStackedGraphs = newStackedGraphs.set(targetGraphId, newStackedGraphs.get(targetGraphId).add(draggedGraphId));
            } else if (newStackedGraphs.has(draggedGraphId)) {
                // draggedGraphId was a stacked graph
                var draggedMergedGraphs = newStackedGraphs.get(draggedGraphId);

                newStackedGraphs = newStackedGraphs.set(targetGraphId, draggedMergedGraphs.add(draggedGraphId));
                newStackedGraphs = newStackedGraphs.delete(draggedGraphId);
            } else {
                // None of the graphs were merged
                newStackedGraphs = newStackedGraphs.set(targetGraphId, Immutable.Set().add(draggedGraphId));
            }

            this.setState({stackedGraphs: newStackedGraphs});
        };
        FieldGraphsStore.onFieldGraphCreated = (graphId) => {
            if (this.notifyOnNewGraphs && !this.initialFieldGraphs.has(graphId)) {
                var element = React.findDOMNode(this.refs[graphId]);
                UIUtils.scrollToHint(element);
            }
        };
    },
    addFieldGraph(field) {
        var streamId = this.props.searchInStream !== undefined ? this.props.searchInStream.id : undefined;
        FieldGraphsStore.newFieldGraph(field, {interval: this.props.resolution, streamid: streamId});
    },
    deleteFieldGraph(graphId) {
        FieldGraphsStore.deleteGraph(graphId);
        if (this.state.stackedGraphs.has(graphId)) {
            this.state.stackedGraphs.get(graphId).forEach((stackedGraphId) => FieldGraphsStore.deleteGraph(stackedGraphId));
            this.setState({stackedGraphs: this.state.stackedGraphs.delete(graphId)});
        }
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
                                      permissions={this.props.permissions}
                                      stacked={this.state.stackedGraphs.has(graphId)}/>
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