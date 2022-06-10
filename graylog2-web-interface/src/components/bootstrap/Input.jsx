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

import InputDescription from 'components/common/InputDescription';

import { Checkbox, Radio } from './imports';
import ControlLabel from './ControlLabel';
import FormControl from './FormControl';
import FormGroup from './FormGroup';
import InputGroup from './InputGroup';
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
    inputDescClassName: PropTypes.string,
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

  getInputDOMNode = () => {
    return this.input;
  };

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
  getChecked = () => {
    return this.getInputDOMNode().checked;
  };

  _renderFormControl = (componentClass, controlProps, children) => {
    return (
      <FormControl inputRef={(ref) => { this.input = ref; }} componentClass={componentClass} {...controlProps}>
        {children}
      </FormControl>
    );
  };

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
    const { id, bsStyle, formGroupClassName, inputDescClassName, wrapperClassName, label, error, help } = this.props;

    return (
      <FormGroup controlId={id} validationState={error ? 'error' : bsStyle} bsClass={formGroupClassName}>
        <InputWrapper className={wrapperClassName}>
          <Checkbox inputRef={(ref) => { this.input = ref; }} {...controlProps}>{label}</Checkbox>
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
      bsStyle,
      formGroupClassName,
      wrapperClassName,
      label,
      labelClassName, inputDescClassName,
      name,
      error,
      help,
      children,
      addonAfter,
      buttonAfter,
      ...controlProps
    } = this.props;

    controlProps.type = type;
    controlProps.label = label;
    controlProps.name = name || id;

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
        return this._renderFormGroup(this._renderFormControl('input', controlProps));
      case 'textarea':
        return this._renderFormGroup(this._renderFormControl('textarea', controlProps));
      case 'select':
        return this._renderFormGroup(this._renderFormControl('select', controlProps, children));
      case 'checkbox':
        return this._renderCheckboxGroup(controlProps);
      case 'radio':
        return this._renderRadioGroup(controlProps);
      default:
        // eslint-disable-next-line no-console
        console.warn(`Unsupported input type ${type}`);
    }

    return null;
  }
}

export default Input;
