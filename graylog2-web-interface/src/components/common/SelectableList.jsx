import React, { PropTypes } from 'react';
import { Button, ListGroup, ListGroupItem } from 'react-bootstrap';

import { Select } from 'components/common';

const SelectableList = React.createClass({
  propTypes: {
    options: PropTypes.any,
    selectedOptionsType: PropTypes.oneOf(['string', 'object']),
    selectedOptions: PropTypes.arrayOf([
      PropTypes.string,
      PropTypes.object,
    ]),
    displayKey: PropTypes.string,
    idKey: PropTypes.string,
    onChange: PropTypes.func,
    autoFocus: PropTypes.bool,
  },

  getDefaultProps() {
    return {
      selectedOptionsType: 'string',
      displayKey: 'label',
      idKey: 'value',
    };
  },

  componentWillReceiveProps(nextProps) {
    if (this.props.selectedOptions !== nextProps.selectedOptions) {
      this.refs.select.clearValue();
    }
  },

  _getOptionId(option) {
    return (typeof option === 'string' ? option : option[this.props.idKey]);
  },

  _getOptionDisplayValue(option) {
    return (typeof option === 'string' ? option : option[this.props.displayKey]);
  },

  _onAddOption(option) {
    if (option === '') {
      return;
    }

    const newSelectedOptions = this.props.selectedOptions.slice();
    if (this.props.selectedOptionsType === 'string') {
      newSelectedOptions.push(option);
    } else {
      newSelectedOptions.push(this.props.options.filter(o => this._getOptionId(o) === option)[0]);
    }

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
        <ListGroupItem key={`${this._getOptionId(option)}-${idx}`}>
          <div className="pull-right">
            <Button bsStyle="primary" bsSize="xsmall" onClick={this._onRemoveOption(idx)}>Remove</Button>
          </div>
          {this._getOptionDisplayValue(option)}
        </ListGroupItem>
      );
    });
    return (
      <div>
        <Select ref="select" autofocus={this.props.autoFocus} options={this.props.options} onValueChange={this._onAddOption} />
        {formattedOptions.length > 0 &&
        <ListGroup style={{ marginTop: 10 }}>{formattedOptions}</ListGroup>
        }
      </div>
    );
  },
});

export default SelectableList;
