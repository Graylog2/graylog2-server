import PropTypes from 'prop-types';
import React from 'react';

import { Checkbox, ControlLabel, FormControl, FormGroup, HelpBlock, InputGroup, Radio } from 'components/graylog';

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
    help: '',
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
      <FormGroup controlId={id} validationState={validationState} bsClass={formGroupClassName}>
        {label && <ControlLabel className={labelClassName}>{label}</ControlLabel>}
        <InputWrapper className={wrapperClassName}>
          {input}
          {help && <HelpBlock>{help}</HelpBlock>}
        </InputWrapper>
      </FormGroup>
    );
  };

  _renderCheckboxGroup = (id, validationState, formGroupClassName, wrapperClassName, label, help, props) => {
    return (
      <FormGroup controlId={id} validationState={validationState} bsClass={formGroupClassName}>
        <InputWrapper className={wrapperClassName}>
          <Checkbox inputRef={(ref) => { this.input = ref; }} {...props}>{label}</Checkbox>
          {help && <HelpBlock>{help}</HelpBlock>}
        </InputWrapper>
      </FormGroup>
    );
  };

  _renderRadioGroup = (id, validationState, formGroupClassName, wrapperClassName, label, help, props) => {
    return (
      <FormGroup controlId={id} validationState={validationState} bsClass={formGroupClassName}>
        <InputWrapper className={wrapperClassName}>
          <Radio inputRef={(ref) => { this.input = ref; }} {...props}>{label}</Radio>
          {help && <HelpBlock>{help}</HelpBlock>}
        </InputWrapper>
      </FormGroup>
    );
  };

  render() {
    const { id, type, bsStyle, formGroupClassName, wrapperClassName, label, labelClassName, name, help, children, addonAfter, buttonAfter, ...controlProps } = this.props;

    controlProps.type = type;
    controlProps.label = label;
    controlProps.name = name || id;

    if (!type) {
      return this._renderFormGroup(id, bsStyle, formGroupClassName, wrapperClassName, label, labelClassName, help, children);
    }

    switch (type) {
      case 'text':
      case 'password':
      case 'email':
      case 'number':
      case 'file':
        return this._renderFormGroup(id, bsStyle, formGroupClassName, wrapperClassName, label, labelClassName, help, this._renderFormControl('input', controlProps), addonAfter, buttonAfter);
      case 'textarea':
        return this._renderFormGroup(id, bsStyle, formGroupClassName, wrapperClassName, label, labelClassName, help, this._renderFormControl('textarea', controlProps));
      case 'select':
        return this._renderFormGroup(id, bsStyle, formGroupClassName, wrapperClassName, label, labelClassName, help, this._renderFormControl('select', controlProps, children));
      case 'checkbox':
        return this._renderCheckboxGroup(id, bsStyle, formGroupClassName, wrapperClassName, label, help, controlProps);
      case 'radio':
        return this._renderRadioGroup(id, bsStyle, formGroupClassName, wrapperClassName, label, help, controlProps);
      default:
        // eslint-disable-next-line no-console
        console.warn(`Unsupported input type ${type}`);
    }

    return null;
  }
}

export default Input;
