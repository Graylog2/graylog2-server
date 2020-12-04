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
import PropTypes from 'prop-types';
import React from 'react';

import {
  InputGroup, FormGroup, ControlLabel, FormControl, HelpBlock, DropdownButton, MenuItem,
} from 'components/graylog';
import { InputWrapper } from 'components/bootstrap';

const OPTIONS = [
  { value: 'STRING', label: 'string' },
  { value: 'NUMBER', label: 'number' },
  { value: 'OBJECT', label: 'object' },
  { value: 'ARRAY', label: 'array' },
  { value: 'BOOLEAN', label: 'boolean' },
  { value: 'NULL', label: 'null' },
];

class JSONValueInput extends React.Component {
  static propTypes = {
    update: PropTypes.func.isRequired,
    label: PropTypes.string,
    help: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
    required: PropTypes.bool,
    validationState: PropTypes.string,
    value: PropTypes.string,
    valueType: PropTypes.oneOf(OPTIONS.map((option) => option.value)),
    allowedTypes: (props, propName, componentName) => {
      // Check that allowedTypes is an array of type values
      const values = OPTIONS.map((option) => option.value);
      const errors = [];

      if (!(props[propName] instanceof Array)) {
        return new Error(`Invalid prop ${propName} supplied to ${componentName}. Expected an array but got ${props[propName]}`);
      }

      props[propName].forEach((p) => {
        if (values.indexOf(p) < 0) {
          errors.push(p);
        }
      });

      if (errors.length > 0) {
        return new Error(`Invalid prop ${propName} supplied to ${componentName}. Expected array of ${values} but got invalid ${errors}`);
      }

      return null;
    },
    labelClassName: PropTypes.string,
    wrapperClassName: PropTypes.string,
  };

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
  };

  UNSAFE_componentWillReceiveProps(nextProps) {
    this.setState(this._computeInitialState(nextProps));
  }

  _computeInitialState = (props) => {
    return {
      value: props.value,
      valueType: props.valueType,
    };
  };

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

  state = this._computeInitialState(this.props);

  render() {
    const options = OPTIONS.filter((o) => this.props.allowedTypes.indexOf(o.value) > -1).map((o) => {
      return <MenuItem key={o.value} onSelect={() => this._onValueTypeSelect(o.value)}>{o.label}</MenuItem>;
    });

    return (
      <FormGroup validationState={this.props.validationState}>
        {this.props.label && <ControlLabel className={this.props.labelClassName}>{this.props.label}</ControlLabel>}
        <InputWrapper className={this.props.wrapperClassName}>
          <InputGroup>
            <FormControl type="text" onChange={this._onUpdate} value={this.state.value} required={this.props.required} />
            <DropdownButton componentClass={InputGroup.Button}
                            id="input-dropdown-addon"
                            bsStyle={this.props.validationState === 'error' ? 'danger' : null}
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
