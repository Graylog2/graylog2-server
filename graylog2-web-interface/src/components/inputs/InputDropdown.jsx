import PropTypes from 'prop-types';
import React from 'react';
import { Button } from 'react-bootstrap';

import { Input } from 'components/bootstrap';
import Spinner from 'components/common/Spinner';

class InputDropdown extends React.Component {
  static propTypes = {
    inputs: PropTypes.object,
    title: PropTypes.string,
    preselectedInputId: PropTypes.string,
    onLoadMessage: PropTypes.func,
    disabled: PropTypes.bool,
  };

  PLACEHOLDER = 'placeholder';

  _formatInput = (input) => {
    return <option key={input.id} value={input.id}>{input.title} ({input.type})</option>;
  };

  _sortByTitle = (input1, input2) => {
    return input1.title.localeCompare(input2.title);
  };

  _onLoadMessage = () => {
    this.props.onLoadMessage(this.state.selectedInput);
  };

  _formatStaticInput = (input) => {
    return (
      <Input id={`${input.type}-select`} type="select" style={{ float: 'left', width: 400, marginRight: 10 }} disabled>
        <option>{`${input.title} (${input.type})`}</option>
      </Input>
    );
  };

  onSelectedInputChange = (event) => {
    this.setState({ selectedInput: event.target.value });
  };

  state = {
    selectedInput: this.props.preselectedInputId || this.PLACEHOLDER,
  };

  render() {
    const { selectedInput } = this.state;
    // When an input is pre-selected, show a static dropdown
    if (this.props.inputs && this.props.preselectedInputId) {
      return (
        <div>
          {this._formatStaticInput(this.props.inputs.get(this.props.preselectedInputId))}

          <Button bsStyle="info"
                  disabled={selectedInput === this.PLACEHOLDER}
                  onClick={this._onLoadMessage}>
            {this.props.title}
          </Button>
        </div>
      );
    }

    if (this.props.inputs) {
      const inputs = this.props.inputs.sort(this._sortByTitle).map(this._formatInput);
      return (
        <div>
          <Input id="placeholder-select"
                 type="select"
                 style={{ float: 'left', width: 400, marginRight: 10 }}
                 value={selectedInput}
                 onChange={this.onSelectedInputChange}
                 placeholder={this.PLACEHOLDER}>
            <option value={this.PLACEHOLDER}>Select an input</option>
            {inputs.toArray()}
          </Input>

          <Button bsStyle="info"
                  disabled={this.props.disabled || selectedInput === this.PLACEHOLDER}
                  onClick={this._onLoadMessage}>
            {this.props.title}
          </Button>
        </div>
      );
    }

    return <Spinner />;
  }
}

export default InputDropdown;
