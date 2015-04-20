'use strict';

var React = require('react/addons');
var OutputsStore = require('../../stores/outputs/OutputsStore');
var StreamsStore = require('../../stores/streams/StreamsStore');
var UserNotification = require("../../util/UserNotification");
var Output = require('./Output');
var $ = require('jquery'); // excluded and shimed

var OutputList = React.createClass({
    OUTPUT_DATA_REFRESH: 5*1000,

    getInitialState() {
        return {
            outputs: [],
            streamId: this.props.streamId,
            permissions: this.props.permissions
        };
    },
    componentDidMount() {
        this.loadData();
    },
    loadData() {
        var callback = (outputs) => {
            if (this.isMounted()) {
                this.setState({
                    outputs: outputs
                });
            }
        };
        if (this.state.streamId) {
            OutputsStore.loadForStreamId(this.state.streamId, callback);
        } else {
            OutputsStore.load(callback);
        }
    },
    _sortByTitle(output1, output2) {
        return output1.title.localeCompare(output2.title);
    },
    removeOutputGlobally(outputId) {
        if (confirm("Do you really want to terminate this output?")) {
            OutputsStore.remove(outputId, (jqXHR, textStatus, errorThrown) => {
                this.loadData();
                UserNotification.success("Output was terminated.", "Success!");
                if (this.props.onUpdate)
                    this.props.onUpdate();
            });
        }
    },
    removeOutputFromStream(outputId, streamId) {
        if (confirm("Do you really want to remove this output from the stream?")) {
            StreamsStore.removeOutput(streamId, outputId, (jqXHR, textStatus, errorThrown) => {
                this.loadData();
                UserNotification.success("Removed output from stream!", "Success!");
                if (this.props.onUpdate)
                    this.props.onUpdate();
            });
        }
    },
    _formatOutput(output) {
        return (<Output key={output.id} output={output} streamId={this.state.streamId} permissions={this.state.permissions}
                        removeOutputFromStream={this.removeOutputFromStream} removeOutputGlobally={this.removeOutputGlobally}
                        onUpdate={this.props.onUpdate} />);
    },
    render() {
        var outputList;
        if (this.state.outputs.length === 0) {
            outputList = (<div className="row content node-row"><div className="col-md-12"><div className="alert alert-info">There are no outputs.</div></div></div>);
        } else {
            var outputs = this.state.outputs.sort(this._sortByTitle).map(this._formatOutput);
            outputList = (
                <div>{outputs}</div>
            );
        }

        return outputList;
    }
});

module.exports = OutputList;
