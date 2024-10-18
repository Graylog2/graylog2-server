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

import InputDescription from 'components/common/InputDescription';
import type { BsSize } from 'components/bootstrap/types';

import Checkbox from './Checkbox';
import { Radio } from './imports';
import ControlLabel from './ControlLabel';
import FormControl from './FormControl';
import FormGroup from './FormGroup';
import InputGroup from './InputGroup';
import InputWrapper from './InputWrapper';

type InputProps = Omit<React.InputHTMLAttributes<HTMLInputElement> & Omit<React.TextareaHTMLAttributes<HTMLTextAreaElement>, 'onChange' | 'onBlur'>, 'value'> & {
  id?: string;
  type?: string;
  name?: string;
  label?: React.ReactElement | string;
  labelClassName?: string;
  bsSize?: BsSize,
  bsStyle?: 'success' | 'warning' | 'error';
  formGroupClassName?: string;
  inputDescClassName?: string;
  value?: string | number | boolean | string[];
  placeholder?: string;
  error?: React.ReactElement | string;
  help?: React.ReactElement | string;
  wrapperClassName?: string;
  addonAfter?: React.ReactElement | string;
  buttonAfter?: React.ReactElement | string;
  children?: any[] | React.ReactElement;
};

/*
 * Input adapter for react bootstrap.
 *
 * The form API in react-bootstrap 0.30 changed quite a lot. This component will serve as an adapter until our
 * code is adapted to the new API.
 *
 */
class Input extends React.Component<InputProps, {
  [key: string]: any;
}> {
  static defaultProps = {
    type: undefined,
    label: '',
    labelClassName: undefined,
    name: undefined,
    formGroupClassName: undefined,
    inputDescClassName: undefined,
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

  private input: HTMLInputElement;

  getInputDOMNode = () => this.input;

  // eslint-disable-next-line react/no-unused-class-component-methods
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

  // eslint-disable-next-line react/no-unused-class-component-methods
  getChecked = () => this.getInputDOMNode().checked;

  _renderFormControl = (componentClass, controlProps, children?) => (
    <FormControl inputRef={(ref) => { this.input = ref; }} componentClass={componentClass} {...controlProps}>
      {children}
    </FormControl>
  );

  _renderFormGroup = (children) => {
    const {
      id,
      bsStyle,
      formGroupClassName,
      wrapperClassName,
      inputDescClassName,
      label,
      labelClassName,
      error,
      help,
      buttonAfter,
      addonAfter,
    } = this.props;

    let input;

    if (addonAfter || buttonAfter) {
      input = (
        <InputGroup>
          {children}
          {buttonAfter && <InputGroup.Button>{buttonAfter}</InputGroup.Button>}
          {addonAfter && <InputGroup.Addon>{addonAfter}</InputGroup.Addon>}
        </InputGroup>
      );
    } else {
      input = children;
    }

    return (
      <FormGroup controlId={id} validationState={error ? 'error' : bsStyle} bsClass={formGroupClassName}>
        {label && <ControlLabel className={labelClassName}>{label}</ControlLabel>}
        <InputWrapper className={wrapperClassName}>
          {input}
          <InputDescription error={error} help={help} className={inputDescClassName} />
        </InputWrapper>
      </FormGroup>
    );
  };

  _renderCheckboxGroup = (controlProps) => {
    const { id, buttonAfter, bsStyle, formGroupClassName, inputDescClassName, wrapperClassName, label, error, help } = this.props;

    return (
      <FormGroup controlId={id} validationState={error ? 'error' : bsStyle} bsClass={formGroupClassName}>
        <InputWrapper className={wrapperClassName}>
          {buttonAfter ? (
            <InputGroup>
              <Checkbox inputRef={(ref) => { this.input = ref; }} {...controlProps}>{label}</Checkbox>
              {buttonAfter && <InputGroup.Button>{buttonAfter}</InputGroup.Button>}
            </InputGroup>
          ) : (
            <Checkbox inputRef={(ref) => { this.input = ref; }} {...controlProps}>{label}</Checkbox>
          )}
          <InputDescription error={error} help={help} className={inputDescClassName} />

        </InputWrapper>
      </FormGroup>
    );
  };

  _renderRadioGroup = (controlProps) => {
    const { id, bsStyle, formGroupClassName, inputDescClassName, wrapperClassName, label, error, help } = this.props;

    return (
      <FormGroup controlId={id} validationState={error ? 'error' : bsStyle} bsClass={formGroupClassName}>
        <InputWrapper className={wrapperClassName}>
          <Radio inputRef={(ref) => { this.input = ref; }} {...controlProps}>{label}</Radio>
          <InputDescription error={error} help={help} className={inputDescClassName} />
        </InputWrapper>
      </FormGroup>
    );
  };

  render() {
    const {
      id,
      type,
      children,
      label,
      name,
      // The following props need to be extracted even if they are not used
      // so they are not passed as controll props to the input
      bsStyle, formGroupClassName, wrapperClassName, labelClassName, inputDescClassName,
      error, help, addonAfter, buttonAfter,
      ...controlProps
    } = this.props;

    const commonProps = {
      type,
      label,
      name: name ?? id,
      ...controlProps,
    };

    if (!type) {
      return this._renderFormGroup(children);
    }

    switch (type) {
      case 'text':
      case 'password':
      case 'email':
      case 'number':
      case 'file':
      case 'tel':
        return this._renderFormGroup(this._renderFormControl('input', commonProps));
      case 'textarea':
        return this._renderFormGroup(this._renderFormControl('textarea', commonProps));
      case 'select':
        return this._renderFormGroup(this._renderFormControl('select', commonProps, children));
      case 'checkbox':
        return this._renderCheckboxGroup(commonProps);
      case 'radio':
        return this._renderRadioGroup(commonProps);
      default:
        // eslint-disable-next-line no-console
        console.warn(`Unsupported input type ${type}`);
    }

    return null;
  }
}

export default Input;
