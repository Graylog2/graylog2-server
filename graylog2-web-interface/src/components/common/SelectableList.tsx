/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import styled from 'styled-components';
import type { SelectInstance } from 'react-select';

import Select from 'components/common/Select';
import { Button, ListGroup, ListGroupItem } from 'components/bootstrap';

const StyledListGroupItem = styled(ListGroupItem)`
  display: flex;
  justify-content: space-between;
`;

type SelectableListProps = {
  /** Options to display in the input. See `Select`'s `options` prop for more information. */
  options?: any[];
  /** Specifies whether `selectedOptions` contains strings or objects. */
  selectedOptionsType?: 'string' | 'object';
  /**
   * Array of string or objects containing the selected options.
   */
  selectedOptions?: string[] | { label: string, value: string }[];
  /** Indicates which option object key contains the text to display in the select input. same as react-select's `labelkey` prop. */
  displayKey?: string;
  /** Indicates which option object key contains the value of the option. */
  idKey?: string;
  /**
   * Function called when an option is added or deleted from the selected options.
   * The function receives an array with selected options as an argument, with
   * the type indicated in `selectedOptionsType`.
   */
  onChange?: (...args: any[]) => void;
  /** Specifies if the input should receive the input focus or not. */
  autoFocus?: boolean;
};

/**
 * Component that renders a `Select` component above a list of selected
 * options.
 *
 * As opposed to `MultiSelect` or `Select`, which display the
 * selected options in the Input itself, this component uses a list,
 * avoiding to clutter the Input when there are too many selected options
 * and/or those options have large names.
 *
 * This component also allows to select the same option many times, and
 * it accepts both arrays of strings and objects as selected options.
 */
class SelectableList extends React.Component<SelectableListProps, {
  [key: string]: any;
}> {
  private select: SelectInstance<unknown, boolean>;

  static defaultProps = {
    autoFocus: undefined,
    displayKey: 'label',
    idKey: 'value',
    onChange: undefined,
    options: undefined,
    selectedOptions: undefined,
    selectedOptionsType: 'string',
  };

  UNSAFE_componentWillReceiveProps(nextProps) {
    if (this.props.selectedOptions !== nextProps.selectedOptions) {
      this.select.clearValue();
    }
  }

  _getOptionId = (option) => (typeof option === 'string' ? option : option[this.props.idKey]);

  _getOptionDisplayValue = (option) => (typeof option === 'string' ? option : option[this.props.displayKey]);

  _onAddOption = (option) => {
    if (option === '') {
      return;
    }

    const newSelectedOptions = this.props.selectedOptions.slice();

    if (this.props.selectedOptionsType === 'string') {
      newSelectedOptions.push(option);
    } else {
      newSelectedOptions.push(this.props.options.filter((o) => this._getOptionId(o) === option)[0]);
    }

    if (typeof this.props.onChange === 'function') {
      this.props.onChange(newSelectedOptions);
    }
  };

  _onRemoveOption = (optionIndex) => () => {
    const newSelectedOptions = this.props.selectedOptions.filter((_, idx) => idx !== optionIndex);

    if (typeof this.props.onChange === 'function') {
      this.props.onChange(newSelectedOptions);
    }
  };

  render() {
    const formattedOptions = this.props.selectedOptions.map((option, idx) => (

      (
        <StyledListGroupItem key={`${this._getOptionId(option)}-${idx}`}>
          <div>
            {this._getOptionDisplayValue(option)}
          </div>
          <Button bsStyle="danger" bsSize="xsmall" onClick={this._onRemoveOption(idx)}>Remove</Button>
        </StyledListGroupItem>
      )
    ));

    return (
      <div>
        <Select ref={(select) => { this.select = select; }} autoFocus={this.props.autoFocus} options={this.props.options} onChange={this._onAddOption} clearable={false} />
        {formattedOptions.length > 0
        && <ListGroup style={{ marginTop: 10 }}>{formattedOptions}</ListGroup>}
      </div>
    );
  }
}

export default SelectableList;
