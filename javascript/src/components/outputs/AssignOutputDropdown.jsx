'use strict';

var React = require('react/addons');
var OutputsStore = require('../../stores/outputs/OutputsStore');
var StreamsStore = require('../../stores/streams/StreamsStore');

var AssignOutputDropdown = React.createClass({
    getInitialState() {
        return {
            allOutputs: [],
            streamOutputs: [],
            streamId: this.props.streamId,
            selectedOutput: "placeholder"
        };
    },
    componentWillReceiveProps(props) {
        this.setState(props);
    },
    loadData() {
        OutputsStore.load((outputs) => {
            this.setState({allOutputs: outputs});
        });
        OutputsStore.loadForStreamId(this.state.streamId, (outputs) => {
            this.setState({streamOutputs: outputs});
        });
    },
    componentDidMount() {
        this.loadData();
    },
    _formatOutput(output) {
        return <option key={output.id} value={output.id}>{output.title}</option>;
    },
    handleUpdate(evt) {
        this.setState({selectedOutput: evt.target.value});
    },
    handleClick(evt) {
        StreamsStore.addOutput(this.state.streamId, this.state.selectedOutput, () => {
            this.loadData();
            this.props.onUpdate();
            this.setState({selectedOutput: "placeholder"});
        });
    },
    render() {
        var streamOutputIds = this.state.streamOutputs.map((output) => {return output.id;});
        var outputs = this.state.allOutputs.filter((output) => {
            for (var i in streamOutputIds) {
                if (output.id === streamOutputIds[i]) {
                    return false;
                }
            }
            return true;
        }).sort((output1, output2) => { return output1.title.localeCompare(output2.title);});
        var outputList = (outputs.length > 0 ? outputs.map(this._formatOutput) : <option disabled>No outputs available</option>);
        var style = {marginTop: '10px'};
        return (
            <div className="output-add" style={style}>
                <form className="form-inline">
                    <select value={this.state.selectedOutput} name="outputId" className="form-control" onChange={this.handleUpdate}>
                        <option value="placeholder" disabled>--- Select existing output ---</option>
                        {outputList}
                    </select>

                    <button type="button" id="add-existing-output" className="btn btn-success form-control" onClick={this.handleClick}>Assign existing Output</button>
                </form>
            </div>
        );
    }
});
module.exports = AssignOutputDropdown;
