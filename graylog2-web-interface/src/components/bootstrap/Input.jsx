import React from 'react';
import { Checkbox, ControlLabel, FormControl, FormGroup, HelpBlock, InputGroup, Radio } from 'react-bootstrap';

import InputWrapper from './InputWrapper';

const generateId = () => {
  console.warn('Input elements should have an id prop, generating one for you');
  const randomNumericId = Math.floor(Math.random() * 1000);
  return `input-${randomNumericId}`;
};

/*
 * Input adapter for react bootstrap.
 *
 * The form API in react-bootstrap 0.30 changed quite a lot. This component will serve as an adapter until our
 * code is adapted to the new API.
 *
 */
const Input = React.createClass({
  propTypes: {
    id: React.PropTypes.string,
    type: React.PropTypes.string,
    label: React.PropTypes.oneOfType([
      React.PropTypes.element,
      React.PropTypes.string,
    ]),
    labelClassName: React.PropTypes.string,
    bsStyle: React.PropTypes.oneOf(['success', 'warning', 'error']),
    value: React.PropTypes.oneOfType([
      React.PropTypes.string,
      React.PropTypes.number,
    ]),
    placeholder: React.PropTypes.string,
    help: React.PropTypes.oneOfType([
      React.PropTypes.element,
      React.PropTypes.string,
    ]),
    wrapperClassName: React.PropTypes.string,
    addonAfter: React.PropTypes.oneOfType([
      React.PropTypes.element,
      React.PropTypes.string,
    ]),
    children: React.PropTypes.oneOfType([
      React.PropTypes.array,
      React.PropTypes.element,
    ]),
  },

  getDefaultProps() {
    return {
      id: generateId(),
      type: undefined,
      label: '',
      labelClassName: undefined,
      bsStyle: null,
      value: undefined,
      placeholder: '',
      help: '',
      wrapperClassName: undefined,
      addonAfter: null,
      children: null,
    };
  },

  getInputDOMNode() {
    return this.input;
  },

  getValue() {
    if (!this.props.type) {
      throw new Error('Cannot use getValue without specifying input type.');
    }

    switch (this.props.type) {
      case 'checkbox':
      case 'radio':
        return this.getInputDOMNode().checked;
      default:
        return this.getInputDOMNode().value;
    }
  },

  getChecked() {
    return this.getInputDOMNode().checked;
  },

  _renderFormControl(componentClass, props, children) {
    return (
      <FormControl inputRef={(ref) => { this.input = ref; }} componentClass={componentClass} {...props}>
        {children}
      </FormControl>
    );
  },

  _renderFormGroup(id, validationState, wrapperClassName, label, labelClassName, help, children, addon) {
    let input;
    if (addon) {
      input = (
        <InputGroup>
          {children}
          <InputGroup.Addon>{addon}</InputGroup.Addon>
        </InputGroup>
      );
    } else {
      input = children;
    }

    return (
      <FormGroup controlId={id} validationState={validationState}>
        {label && <ControlLabel className={labelClassName}>{label}</ControlLabel>}
        <InputWrapper className={wrapperClassName}>
          {input}
          {help && <HelpBlock>{help}</HelpBlock>}
        </InputWrapper>
      </FormGroup>
    );
  },

  _renderCheckboxGroup(id, validationState, wrapperClassName, label, help, props) {
    return (
      <FormGroup controlId={id} validationState={validationState}>
        <InputWrapper className={wrapperClassName}>
          <Checkbox inputRef={(ref) => { this.input = ref; }} {...props}>{label}</Checkbox>
          {help && <HelpBlock>{help}</HelpBlock>}
        </InputWrapper>
      </FormGroup>
    );
  },

  _renderRadioGroup(id, validationState, wrapperClassName, label, help, props) {
    return (
      <FormGroup controlId={id} validationState={validationState}>
        <InputWrapper className={wrapperClassName}>
          <Radio inputRef={(ref) => { this.input = ref; }} {...props}>{label}</Radio>
          {help && <HelpBlock>{help}</HelpBlock>}
        </InputWrapper>
      </FormGroup>
    );
  },

  render() {
    const { id, type, bsStyle, wrapperClassName, label, labelClassName, help, children, addonAfter, ...controlProps } = this.props;
    controlProps.type = type;
    controlProps.label = label;

    if (!type) {
      return this._renderFormGroup(id, bsStyle, wrapperClassName, label, labelClassName, help, children);
    }

    switch (type) {
      case 'text':
      case 'password':
      case 'email':
      case 'number':
      case 'file':
        return this._renderFormGroup(id, bsStyle, wrapperClassName, label, labelClassName, help, this._renderFormControl('input', controlProps), addonAfter);
      case 'textarea':
        return this._renderFormGroup(id, bsStyle, wrapperClassName, label, labelClassName, help, this._renderFormControl('textarea', controlProps));
      case 'select':
        return this._renderFormGroup(id, bsStyle, wrapperClassName, label, labelClassName, help, this._renderFormControl('select', controlProps, children));
      case 'checkbox':
        return this._renderCheckboxGroup(id, bsStyle, wrapperClassName, label, help, controlProps);
      case 'radio':
        return this._renderRadioGroup(id, bsStyle, wrapperClassName, label, help, controlProps);
      default:
        console.warn(`Unsupported input type ${type}`);
    }

    return null;
  },
});

export default Input;
