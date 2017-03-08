import React from 'react';
import ReactDOM from 'react-dom';
import { Checkbox, ControlLabel, FormControl, FormGroup, HelpBlock, Radio } from 'react-bootstrap';

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
    value: React.PropTypes.string,
    multiple: React.PropTypes.bool,
    placeholder: React.PropTypes.string,
    help: React.PropTypes.oneOfType([
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
      value: undefined,
      multiple: false,
      placeholder: '',
      help: '',
      children: null,
    };
  },

  componentWillMount() {
    console.warn('Please consider migrating the Input element into the new react-bootstrap API: https://react-bootstrap.github.io/components.html#forms');
  },

  getInputDOMNode() {
    return ReactDOM.findDOMNode(this.input);
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

  _renderFormControl(componentClass, props, children) {
    return (
      <FormControl ref={(ref) => { this.input = ref; }} componentClass={componentClass} {...props}>
        {children}
      </FormControl>
    );
  },

  _renderFormGroup(id, label, help, children) {
    return (
      <FormGroup controlId={id}>
        {label && <ControlLabel>{label}</ControlLabel>}
        {children}
        {help && <HelpBlock>{help}</HelpBlock>}
      </FormGroup>
    );
  },

  _renderCheckboxGroup(id, label, help, props) {
    return (
      <FormGroup controlId={id}>
        <Checkbox ref={(ref) => { this.input = ref; }} {...props}>{label}</Checkbox>
        {help && <HelpBlock>{help}</HelpBlock>}
      </FormGroup>
    );
  },

  _renderRadioGroup(id, label, help, props) {
    return (
      <FormGroup controlId={id}>
        <Radio ref={(ref) => { this.input = ref; }} {...props}>{label}</Radio>
        {help && <HelpBlock>{help}</HelpBlock>}
      </FormGroup>
    );
  },

  render() {
    const { id, type, label, help, children, ...controlProps } = this.props;
    controlProps.type = type;

    if (!type) {
      return this._renderFormGroup(id, label, help, children);
    }

    switch (type) {
      case 'text':
      case 'password':
      case 'email':
      case 'number':
      case 'file':
        return this._renderFormGroup(id, label, help, this._renderFormControl('input', controlProps));
      case 'textarea':
        return this._renderFormGroup(id, label, help, this._renderFormControl('textarea', controlProps));
      case 'select':
        return this._renderFormGroup(id, label, help, this._renderFormControl('select', controlProps, children));
      case 'checkbox':
        return this._renderCheckboxGroup(id, label, help, controlProps);
      case 'radio':
        return this._renderRadioGroup(id, label, help, controlProps);
      default:
        console.warn(`Unsupported input type ${type}`);
    }

    return null;
  },
});

export default Input;
