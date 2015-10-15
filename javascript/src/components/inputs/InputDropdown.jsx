import React from 'react';
import LinkedStateMixin from 'react-addons-linked-state-mixin';
import { Input } from 'react-bootstrap';
import Immutable from 'immutable';

import Spinner from 'components/common/Spinner';

const InputDropdown = React.createClass({
  mixins: [LinkedStateMixin],
  PLACEHOLDER: "placeholder",
  getInitialState() {
    return {
      selectedInput: this.PLACEHOLDER
    };
  },
  _formatInput(input) {
    return <option key={input.input_id} value={input.input_id}>{input.title} ({input.type})</option>;
  },
  _sortByTitle(input1, input2) {
    return input1.title.localeCompare(input2.title);
  },
  _onClick() {
    this.props.onClick(this.state.selectedInput);
  },
  render() {
    if (!this.props.inputs) {
      return <Spinner />;
    }
    const inputs = Immutable.List(this.props.inputs.sort(this._sortByTitle).values()).map(this._formatInput);
    return (
      <div>
        <Input type='select' style={{float: "left", width: "400px", marginRight: "10px"}} valueLink={this.linkState('selectedInput')} placeholder={this.PLACEHOLDER}>
          <option value={this.PLACEHOLDER}>Select an input</option>
          {inputs}
        </Input>

        <a className="btn btn-info" disabled={this.state.selectedInput === this.PLACEHOLDER} onClick={this._onClick}>{this.props.title}</a>
      </div>
    );
  }
});

export default InputDropdown;
