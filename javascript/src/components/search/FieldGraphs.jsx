'use strict';

var React = require('react');
var Immutable = require('immutable');

var LegacyFieldGraph = require('./LegacyFieldGraph');
var FieldGraphsStore = require('../../stores/field-analyzers/FieldGraphsStore');
var UIUtils = require('../../util/UIUtils');

var FieldGraphs = React.createClass({
    getInitialState() {
        this.newGraph = undefined;

        return {
            fieldGraphs: Immutable.Map()
        };
    },
    componentDidMount() {
        this.setState({fieldGraphs: FieldGraphsStore.fieldGraphs});
        FieldGraphsStore.onFieldGraphsUpdated = (newFieldGraphs) => this.setState({fieldGraphs: newFieldGraphs});
        FieldGraphsStore.onNewFieldGraph = (graphId) => { this.newGraph = graphId };
    },
    componentDidUpdate() {
        if (this.newGraph !== undefined) {
            var element = React.findDOMNode(this.refs[this.newGraph]);
            UIUtils.scrollToHint(element);
            this.newGraph = undefined;
        }
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