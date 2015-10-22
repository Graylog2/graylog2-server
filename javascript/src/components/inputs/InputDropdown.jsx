import React, {PropTypes} from 'react';
import LinkedStateMixin from 'react-addons-linked-state-mixin';
import { Input } from 'react-bootstrap';
import Immutable from 'immutable';

import Spinner from 'components/common/Spinner';

const InputDropdown = React.createClass({
  propTypes: {
    inputs: PropTypes.object,
    title: PropTypes.string,
    onLoadMessage: PropTypes.func,
  },
  mixins: [LinkedStateMixin],
  getInitialState() {
    return {
      selectedInput: this.PLACEHOLDER,
    };
  },
  PLACEHOLDER: 'placeholder',
  _formatInput(input) {
    return <option key={input.input_id} value={input.input_id}>{input.title} ({input.type})</option>;
  },
  _sortByTitle(input1, input2) {
    return input1.title.localeCompare(input2.title);
  },
  _onLoadMessage() {
    this.props.onLoadMessage(this.state.selectedInput);
  },
  render() {
    if (!this.props.inputs) {
      return <Spinner />;
    }
    const inputs = Immutable.List(this.props.inputs.sort(this._sortByTitle).values()).map(this._formatInput);
    return (
      <div>
        <Input type="select" style={{float: 'left', width: 400, marginRight: 10}}
               valueLink={this.linkState('selectedInput')} placeholder={this.PLACEHOLDER}>
          <option value={this.PLACEHOLDER}>Select an input</option>
          {inputs}
        </Input>

        <a className="btn btn-info" disabled={this.state.selectedInput === this.PLACEHOLDER}
           onClick={this._onLoadMessage}>{this.props.title}</a>
      </div>
    );
  },
});

export default InputDropdown;
