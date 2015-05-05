'use strict';

var React = require('react');

var LegacyFieldGraph = require('./LegacyFieldGraph');

var FieldGraphs = React.createClass({
    render() {
        var fieldGraphs = this.props.dataFields.map((field) => {
            return <LegacyFieldGraph key={field} field={field} from={this.props.from} to={this.props.to}/>;
        });

        return (
            <div id="field-graphs">
                {fieldGraphs}
            </div>
        );
    }
});

module.exports = FieldGraphs;