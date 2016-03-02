import React from 'react';

import { Select } from 'components/common';

const SelectableList = React.createClass({
  propTypes: {
    options: React.PropTypes.any,
    onChange: React.PropTypes.func,
  },

  getInitialState() {
    return {
      selectedOptions: [],
    };
  },

  _onAddOption(option) {
    if (option === '') {
      return;
    }

    const newSelectedOptions = this.state.selectedOptions.slice();
    newSelectedOptions.push(option);
    this.setState({selectedOptions: newSelectedOptions});
    if (typeof this.props.onChange === 'function') {
      this.props.onChange(newSelectedOptions);
    }
  },

  render() {
    const formattedOptions = this.state.selectedOptions.map((option, idx) => <li key={`${option}-${idx}`}>{option}</li>);
    return (
      <div>
        <Select options={this.props.options} onValueChange={this._onAddOption}/>
        {formattedOptions.length > 0 &&
        <ul>{formattedOptions}</ul>
        }
      </div>
    );
  },
});

export default SelectableList;
