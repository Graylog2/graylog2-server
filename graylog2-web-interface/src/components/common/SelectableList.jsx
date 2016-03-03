import React from 'react';
import { Button, ListGroup, ListGroupItem } from 'react-bootstrap';

import { Select } from 'components/common';

const SelectableList = React.createClass({
  propTypes: {
    options: React.PropTypes.any,
    selectedOptions: React.PropTypes.array,
    onChange: React.PropTypes.func,
  },

  _onAddOption(option) {
    if (option === '') {
      return;
    }

    const newSelectedOptions = this.props.selectedOptions.slice();
    newSelectedOptions.push(option);
    if (typeof this.props.onChange === 'function') {
      this.props.onChange(newSelectedOptions);
    }
  },

  _onRemoveOption(optionIndex) {
    return () => {
      const newSelectedOptions = this.props.selectedOptions.filter((_, idx) => idx !== optionIndex);
      if (typeof this.props.onChange === 'function') {
        this.props.onChange(newSelectedOptions);
      }
    };
  },

  render() {
    const formattedOptions = this.props.selectedOptions.map((option, idx) => {
      return (
        <ListGroupItem key={`${option}-${idx}`}>
          <div className="pull-right">
            <Button bsStyle="primary" bsSize="xsmall" onClick={this._onRemoveOption(idx)}>Remove</Button>
          </div>
          {option}
        </ListGroupItem>
      );
    });
    return (
      <div>
        <Select options={this.props.options} onValueChange={this._onAddOption}/>
        {formattedOptions.length > 0 &&
        <ListGroup style={{marginTop: 10}}>{formattedOptions}</ListGroup>
        }
      </div>
    );
  },
});

export default SelectableList;
