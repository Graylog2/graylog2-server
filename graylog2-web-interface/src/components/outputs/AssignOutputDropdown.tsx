import React from 'react';

import { Button } from 'components/bootstrap';

type AssignOutputDropdownProps = {
  outputs: any[];
  onSubmit: (...args: any[]) => void;
};

class AssignOutputDropdown extends React.Component<AssignOutputDropdownProps, {
  [key: string]: any;
}> {
  PLACEHOLDER = 'placeholder';

  state = {
    selectedOutput: this.PLACEHOLDER,
  };

  _formatOutput = (output) => <option key={output.id} value={output.id}>{output.title}</option>;

  _handleUpdate = (evt) => {
    this.setState({ selectedOutput: evt.target.value });
  };

  _handleClick = () => {
    const { onSubmit } = this.props;
    const { selectedOutput } = this.state;

    onSubmit(selectedOutput);
    this.setState({ selectedOutput: this.PLACEHOLDER });
  };

  render() {
    const { outputs } = this.props;
    const { selectedOutput } = this.state;
    const outputList = (outputs.length > 0
      ? outputs.map(this._formatOutput)
      : <option disabled>No outputs available</option>);

    return (
      <div className="output-add">
        <div className="form-inline">
          <select value={selectedOutput}
                  name="outputId"
                  className="form-control"
                  onChange={this._handleUpdate}>
            <option value={this.PLACEHOLDER} disabled>Select existing output</option>
            {outputList}
          </select>
          &nbsp;
          <Button id="add-existing-output"
                  bsStyle="success"
                  type="button"
                  disabled={selectedOutput === this.PLACEHOLDER}
                  onClick={this._handleClick}>
            Assign existing Output
          </Button>
        </div>
      </div>
    );
  }
}

export default AssignOutputDropdown;