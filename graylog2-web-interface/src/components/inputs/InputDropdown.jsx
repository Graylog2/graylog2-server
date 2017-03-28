import React, { PropTypes } from 'react';
import LinkedStateMixin from 'react-addons-linked-state-mixin';
import { Button } from 'react-bootstrap';

import { Input } from 'components/bootstrap';
import Spinner from 'components/common/Spinner';

const InputDropdown = React.createClass({
  propTypes: {
    inputs: PropTypes.object,
    title: PropTypes.string,
    preselectedInputId: PropTypes.string,
    onLoadMessage: PropTypes.func,
    disabled: PropTypes.bool,
  },
  mixins: [LinkedStateMixin],
  getInitialState() {
    return {
      selectedInput: this.props.preselectedInputId || this.PLACEHOLDER,
    };
  },
  PLACEHOLDER: 'placeholder',
  _formatInput(input) {
    return <option key={input.id} value={input.id}>{input.title} ({input.type})</option>;
  },
  _sortByTitle(input1, input2) {
    return input1.title.localeCompare(input2.title);
  },
  _onLoadMessage() {
    this.props.onLoadMessage(this.state.selectedInput);
  },
  _formatStaticInput(input) {
    return (
      <Input type="select" style={{ float: 'left', width: 400, marginRight: 10 }} disabled>
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

          <Button bsStyle="info" disabled={this.state.selectedInput === this.PLACEHOLDER}
             onClick={this._onLoadMessage}>{this.props.title}</Button>
        </div>
      );
    }

    if (this.props.inputs) {
      const inputs = this.props.inputs.sort(this._sortByTitle).map(this._formatInput);
      return (
        <div>
          <Input type="select" style={{ float: 'left', width: 400, marginRight: 10 }}
                 valueLink={this.linkState('selectedInput')} placeholder={this.PLACEHOLDER}>
            <option value={this.PLACEHOLDER}>Select an input</option>
            {inputs.toArray()}
          </Input>

          <Button bsStyle="info" disabled={this.props.disabled || this.state.selectedInput === this.PLACEHOLDER}
             onClick={this._onLoadMessage}>{this.props.title}</Button>
        </div>
      );
    }

    return <Spinner />;
  },
});

export default InputDropdown;
