import React, {PropTypes} from 'react';
import LinkedStateMixin from 'react-addons-linked-state-mixin';
import { Input } from 'react-bootstrap';
import Immutable from 'immutable';

import Spinner from 'components/common/Spinner';

const InputDropdown = React.createClass({
  propTypes: {
    inputs: PropTypes.object,
    title: PropTypes.string,
    preselectedInputId: PropTypes.string,
    onLoadMessage: PropTypes.func,
  },
  mixins: [LinkedStateMixin],
  getInitialState() {
    return {
      selectedInput: this.props.preselectedInputId || this.PLACEHOLDER,
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
  _formatStaticInput(input) {
    return (
      <Input type="select" style={{float: 'left', width: 400, marginRight: 10}} disabled>
        <option>{`${input.title} (${input.type})`}</option>
      </Input>
    );
  },
  render() {
    // When an input is pre-selected, show a static dropdown
    if (this.props.inputs && this.props.preselectedInputId) {
      return (
        <div>
          {this._formatStaticInput(this.props.inputs.get(this.props.preselectedInputId))}

          <a className="btn btn-info" disabled={this.state.selectedInput === this.PLACEHOLDER}
             onClick={this._onLoadMessage}>{this.props.title}</a>
        </div>
      );
    }

    if (this.props.inputs) {
      const inputs = Immutable.List(this.props.inputs.sort(this._sortByTitle).map(this._formatInput));
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
    }

    return <Spinner/>;
  },
});

export default InputDropdown;
