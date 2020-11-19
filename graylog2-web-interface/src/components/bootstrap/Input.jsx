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

import { Checkbox, ControlLabel, FormControl, FormGroup, InputGroup, Radio } from 'components/graylog';
import InputDescription from 'components/common/InputDescription';

import InputWrapper from './InputWrapper';

/*
 * Input adapter for react bootstrap.
 *
 * The form API in react-bootstrap 0.30 changed quite a lot. This component will serve as an adapter until our
 * code is adapted to the new API.
 *
 */
class Input extends React.Component {
  static propTypes = {
    id: PropTypes.string.isRequired,
    type: PropTypes.string,
    name: PropTypes.string,
    label: PropTypes.oneOfType([
      PropTypes.element,
      PropTypes.string,
    ]),
    labelClassName: PropTypes.string,
    bsStyle: PropTypes.oneOf(['success', 'warning', 'error']),
    formGroupClassName: PropTypes.string,
    value: PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.number,
    ]),
    placeholder: PropTypes.string,
    error: PropTypes.oneOfType([
      PropTypes.element,
      PropTypes.string,
    ]),
    help: PropTypes.oneOfType([
      PropTypes.element,
      PropTypes.string,
    ]),
    wrapperClassName: PropTypes.string,
    addonAfter: PropTypes.oneOfType([
      PropTypes.element,
      PropTypes.string,
    ]),
    buttonAfter: PropTypes.oneOfType([
      PropTypes.element,
      PropTypes.string,
    ]),
    children: PropTypes.oneOfType([
      PropTypes.array,
      PropTypes.element,
    ]),
  };

  static defaultProps = {
    type: undefined,
    label: '',
    labelClassName: undefined,
    name: undefined,
    formGroupClassName: undefined,
    bsStyle: null,
    value: undefined,
    placeholder: '',
    error: undefined,
    help: undefined,
    wrapperClassName: undefined,
    addonAfter: null,
    buttonAfter: null,
    children: null,
  };

  getInputDOMNode = () => {
    return this.input;
  };

  getValue = () => {
    const { type } = this.props;

    if (!type) {
      throw new Error('Cannot use getValue without specifying input type.');
    }

    switch (type) {
      case 'checkbox':
      case 'radio':
        return this.getInputDOMNode().checked;
      default:
        return this.getInputDOMNode().value;
    }
  };

  getChecked = () => {
    return this.getInputDOMNode().checked;
  };

  _renderFormControl = (componentClass, props, children) => {
    return (
      <FormControl inputRef={(ref) => { this.input = ref; }} componentClass={componentClass} {...props}>
        {children}
      </FormControl>
    );
  };

  _renderFormGroup = (
    id,
    validationState,
    formGroupClassName,
    wrapperClassName,
    label,
    labelClassName,
    error,
    help,
    children,
    addon,
    button,
  ) => {
    let input;

    if (addon || button) {
      input = (
        <InputGroup>
          {children}
          {button && <InputGroup.Button>{button}</InputGroup.Button>}
          {addon && <InputGroup.Addon>{addon}</InputGroup.Addon>}
        </InputGroup>
      );
    } else {
      input = children;
    }

    return (
      <FormGroup controlId={id} validationState={error ? 'error' : validationState} bsClass={formGroupClassName}>
        {label && <ControlLabel className={labelClassName}>{label}</ControlLabel>}
        <InputWrapper className={wrapperClassName}>
          {input}
          <InputDescription error={error} help={help} />
        </InputWrapper>
      </FormGroup>
    );
  };

  _renderCheckboxGroup = (id, validationState, formGroupClassName, wrapperClassName, label, error, help, props) => {
    return (
      <FormGroup controlId={id} validationState={error ? 'error' : validationState} bsClass={formGroupClassName}>
        <InputWrapper className={wrapperClassName}>
          <Checkbox inputRef={(ref) => { this.input = ref; }} {...props}>{label}</Checkbox>
          <InputDescription error={error} help={help} />
        </InputWrapper>
      </FormGroup>
    );
  };

  _renderRadioGroup = (id, validationState, formGroupClassName, wrapperClassName, label, error, help, props) => {
    return (
      <FormGroup controlId={id} validationState={error ? 'error' : validationState} bsClass={formGroupClassName}>
        <InputWrapper className={wrapperClassName}>
          <Radio inputRef={(ref) => { this.input = ref; }} {...props}>{label}</Radio>
          <InputDescription error={error} help={help} />
        </InputWrapper>
      </FormGroup>
    );
  };

  render() {
    const { id, type, bsStyle, formGroupClassName, wrapperClassName, label, labelClassName, name, error, help, children, addonAfter, buttonAfter, ...controlProps } = this.props;

    controlProps.type = type;
    controlProps.label = label;
    controlProps.name = name || id;

    if (!type) {
      return this._renderFormGroup(id, bsStyle, formGroupClassName, wrapperClassName, label, labelClassName, error, help, children);
    }

    switch (type) {
      case 'text':
      case 'password':
      case 'email':
      case 'number':
      case 'file':
      case 'tel':
        return this._renderFormGroup(id, bsStyle, formGroupClassName, wrapperClassName, label, labelClassName, error, help, this._renderFormControl('input', controlProps), addonAfter, buttonAfter);
      case 'textarea':
        return this._renderFormGroup(id, bsStyle, formGroupClassName, wrapperClassName, label, labelClassName, error, help, this._renderFormControl('textarea', controlProps));
      case 'select':
        return this._renderFormGroup(id, bsStyle, formGroupClassName, wrapperClassName, label, labelClassName, error, help, this._renderFormControl('select', controlProps, children));
      case 'checkbox':
        return this._renderCheckboxGroup(id, bsStyle, formGroupClassName, wrapperClassName, label, error, help, controlProps);
      case 'radio':
        return this._renderRadioGroup(id, bsStyle, formGroupClassName, wrapperClassName, label, error, help, controlProps);
      default:
        // eslint-disable-next-line no-console
        console.warn(`Unsupported input type ${type}`);
    }

    return null;
  }
}

export default Input;
