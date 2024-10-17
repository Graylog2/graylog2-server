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

import {
  ControlLabel,
  DropdownButton,
  FormControl,
  FormGroup,
  HelpBlock,
  InputGroup,
  InputWrapper,
  MenuItem,
} from 'components/bootstrap';

const OPTIONS = [
  { value: 'STRING', label: 'string' },
  { value: 'NUMBER', label: 'number' },
  { value: 'OBJECT', label: 'object' },
  { value: 'ARRAY', label: 'array' },
  { value: 'BOOLEAN', label: 'boolean' },
  { value: 'NULL', label: 'null' },
];

type JSONValueInputProps = {
  update: (...args: any[]) => void;
  onBlur?: (...args: any[]) => void;
  label?: string;
  help?: string | any;
  required?: boolean;
  validationState?: 'error' | 'success' | 'warning';
  value?: string;
  valueType?: string;
  allowedTypes?: any;
  labelClassName?: string;
  wrapperClassName?: string;
};

class JSONValueInput extends React.Component<JSONValueInputProps, {
  [key: string]: any;
}> {
  static defaultProps = {
    value: '',
    valueType: 'STRING',
    allowedTypes: OPTIONS.map((option) => option.value),
    label: '',
    help: '',
    required: false,
    validationState: null,
    labelClassName: undefined,
    wrapperClassName: undefined,
    onBlur: undefined,
  };

  constructor(props) {
    super(props);
    this.state = this._computeInitialState();
  }

  UNSAFE_componentWillReceiveProps() {
    this.setState(this._computeInitialState());
  }

  _computeInitialState = () => ({
    value: this.props.value,
    valueType: this.props.valueType,
  });

  _propagateState = () => {
    this.props.update(this.state.value, this.state.valueType);
  };

  _onUpdate = (e) => {
    const { value } = e.target;

    this.setState({ value: value }, this._propagateState);
  };

  _onValueTypeSelect = (valueType) => {
    this.setState({ valueType: valueType }, this._propagateState);
  };

  render() {
    const options = OPTIONS.filter((o) => this.props.allowedTypes.indexOf(o.value) > -1).map((o) => <MenuItem key={o.value} onSelect={() => this._onValueTypeSelect(o.value)}>{o.label}</MenuItem>);

    return (
      <FormGroup validationState={this.props.validationState}>
        {this.props.label && <ControlLabel className={this.props.labelClassName}>{this.props.label}</ControlLabel>}
        <InputWrapper className={this.props.wrapperClassName}>
          <InputGroup>
            <FormControl type="text" onChange={this._onUpdate} onBlur={this.props.onBlur} value={this.state.value} required={this.props.required} />
            <DropdownButton id="input-dropdown-addon"
                            bsStyle={this.props.validationState === 'error' ? 'danger' : 'default'}
                            title={OPTIONS.filter((o) => o.value === this.props.valueType)[0].label}>
              {options}
            </DropdownButton>
          </InputGroup>
          {this.props.help && <HelpBlock>{this.props.help}</HelpBlock>}
        </InputWrapper>
      </FormGroup>
    );
  }
}

export default JSONValueInput;
