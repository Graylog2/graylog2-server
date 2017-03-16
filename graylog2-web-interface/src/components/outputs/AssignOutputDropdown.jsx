import React from 'react';
import { Button } from 'react-bootstrap';

const AssignOutputDropdown = React.createClass({
  propTypes: {
    outputs: React.PropTypes.array.isRequired,
    onSubmit: React.PropTypes.func.isRequired,
  },
  getInitialState() {
    return {
      selectedOutput: this.PLACEHOLDER,
    };
  },
  PLACEHOLDER: 'placeholder',
  _formatOutput(output) {
    return <option key={output.id} value={output.id}>{output.title}</option>;
  },
  _handleUpdate(evt) {
    this.setState({ selectedOutput: evt.target.value });
  },
  _handleClick() {
    this.props.onSubmit(this.state.selectedOutput);
    this.setState({ selectedOutput: this.PLACEHOLDER });
  },
  render() {
    const outputs = this.props.outputs;
    const outputList = (outputs.length > 0 ? outputs.map(this._formatOutput) :
    <option disabled>No outputs available</option>);
    return (
      <div className="output-add">
        <div className="form-inline">
          <select value={this.state.selectedOutput} name="outputId" className="form-control"
                  onChange={this._handleUpdate}>
            <option value={this.PLACEHOLDER} disabled>Select existing output</option>
            {outputList}
          </select>
          &nbsp;
          <Button ref="submitButton" id="add-existing-output" bsStyle="success" type="button"
                  disabled={this.state.selectedOutput === this.PLACEHOLDER} onClick={this._handleClick}>
            Assign existing Output
          </Button>
        </div>
      </div>
    );
  },
});

export default AssignOutputDropdown;
