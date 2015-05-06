'use strict';

var React = require('react');
var Immutable = require('immutable');

var LegacyFieldGraph = require('./LegacyFieldGraph');
var FieldGraphsStore = require('../../stores/field-graphs/FieldGraphsStore');

var FieldGraphs = React.createClass({
    getInitialState() {
        return {
            fieldGraphs: Immutable.Map()
        };
    },
    componentDidMount() {
        this.setState({fieldGraphs: FieldGraphsStore.fieldGraphs});
        FieldGraphsStore.onFieldGraphsUpdated = (newFieldGraphs) => this.setState({fieldGraphs: newFieldGraphs});
    },
    addFieldGraph(field) {
        FieldGraphsStore.newFieldGraph(field);
    },
    render() {
        var fieldGraphs = [];

        this.state.fieldGraphs.forEach((graphOptions, graphId) => {
            fieldGraphs.push(
                <LegacyFieldGraph key={graphId} graphId={graphId} graphOptions={graphOptions} from={this.props.from} to={this.props.to}/>
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