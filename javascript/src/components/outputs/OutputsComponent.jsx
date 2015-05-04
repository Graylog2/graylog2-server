'use strict';

var React = require('react/addons');
var OutputList = require('./OutputList');
var CreateOutputDropdown = require('./CreateOutputDropdown');
var AssignOutputDropdown = require('./AssignOutputDropdown');
var OutputsStore = require('../../stores/outputs/OutputsStore');
var StreamsStore = require('../../stores/streams/StreamsStore');
var UserNotification = require("../../util/UserNotification");
var PermissionsMixin = require('../../util/PermissionsMixin');

var OutputComponent = React.createClass({
    mixins: [PermissionsMixin],
    componentDidMount() {
        this.loadData();
    },
    loadData() {
        var callback = (outputs) => {
            this.setState({
                outputs: outputs
            });
            if (this.props.streamId) {
                this._fetchAssignableOutputs();
            }
        };
        if (this.props.streamId) {
            OutputsStore.loadForStreamId(this.props.streamId, callback);
        } else {
            OutputsStore.load(callback);
        }

        OutputsStore.loadAvailableTypes((types) => {
            this.setState({types:types});
        });
        this._fetchAssignableOutputs();
    },
    getInitialState() {
      return {
          outputs: [],
          assignableOutputs: [],
          types: {}
      };
    },
    _handleUpdate() {
        this.loadData();
    },
    _handleCreateOutput(data) {
        OutputsStore.save(data, (result) => {
            this.setState({typeName: "placeholder"});
            if (this.props.streamId) {
                StreamsStore.addOutput(this.props.streamId, result.id, () => {
                    this._handleUpdate();
                });
            } else {
                this._handleUpdate();
            }
        });
    },
    _fetchAssignableOutputs() {
        OutputsStore.load((allOutputs) => {
            var streamOutputIds = this.state.outputs.map((output) => {return output.id;});
            var outputs = allOutputs.filter((output) => {
                for (var i in streamOutputIds) {
                    if (output.id === streamOutputIds[i]) {
                        return false;
                    }
                }
                return true;
            }).sort((output1, output2) => { return output1.title.localeCompare(output2.title);});
            this.setState({assignableOutputs: outputs});
        });
    },
    _handleAssignOutput(outputId) {
        StreamsStore.addOutput(this.props.streamId, outputId, () => {
            this._handleUpdate();
        });
    },
    _removeOutputGlobally(outputId) {
        if (window.confirm("Do you really want to terminate this output?")) {
            OutputsStore.remove(outputId, (jqXHR, textStatus, errorThrown) => {
                UserNotification.success("Output was terminated.", "Success!");
                this._handleUpdate();
            });
        }
    },
    _removeOutputFromStream(outputId, streamId) {
        if (window.confirm("Do you really want to remove this output from the stream?")) {
            StreamsStore.removeOutput(streamId, outputId, (jqXHR, textStatus, errorThrown) => {
                UserNotification.success("Removed output from stream!", "Success!");
                this._handleUpdate();
            });
        }
    },
    _handleOutputUpdate(output, deltas) {
        OutputsStore.update(output, deltas, () => {
            this._handleUpdate();
        });
    },
    render() {
        var permissions = this.props.permissions;
        var streamId = this.props.streamId;
        var createOutputDropdown = (this.isPermitted(permissions, ["outputs:create"]) ?
            <CreateOutputDropdown types={this.state.types} onSubmit={this._handleCreateOutput} getTypeDefinition={OutputsStore.loadAvailable} streamId={streamId}/> : "");
        var assignOutputDropdown = (streamId ?
            <AssignOutputDropdown ref="assignOutputDropdown" streamId={streamId} outputs={this.state.assignableOutputs} onSubmit={this._handleAssignOutput}/> : "");
        return (<div className="outputs">
                    <div className="row node-row input-new content">
                        <div className="col-md-12">
                            {createOutputDropdown}
                            {assignOutputDropdown}
                        </div>
                    </div>

                    <OutputList ref="outputList" streamId={streamId} outputs={this.state.outputs} permissions={permissions} getTypeDefinition={OutputsStore.loadAvailable}
                        onRemove={this._removeOutputFromStream} onTerminate={this._removeOutputGlobally} onUpdate={this._handleOutputUpdate}/>
                </div>);
    }
});
module.exports = OutputComponent;

