'use strict';

var React = require('react/addons');
var Output = require('./Output');

var OutputList = React.createClass({
    OUTPUT_DATA_REFRESH: 5*1000,

    _sortByTitle(output1, output2) {
        return output1.title.localeCompare(output2.title);
    },
    _formatOutput(output) {
        return (<Output key={output.id} output={output} streamId={this.props.streamId} permissions={this.props.permissions}
                        removeOutputFromStream={this.props.onRemove} removeOutputGlobally={this.props.onTerminate}
                        onUpdate={this.props.onUpdate} getTypeDefinition={this.props.getTypeDefinition} />);
    },
    render() {
        var outputList;
        if (this.props.outputs.length === 0) {
            outputList = (<div className="row content"><div className="col-md-12"><div className="alert alert-info">No outputs configured.</div></div></div>);
        } else {
            var outputs = this.props.outputs.sort(this._sortByTitle).map(this._formatOutput);
            outputList = (
                <div>{outputs}</div>
            );
        }

        return outputList;
    }
});

module.exports = OutputList;
