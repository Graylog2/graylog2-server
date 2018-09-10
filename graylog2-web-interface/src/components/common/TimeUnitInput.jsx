import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import {
  InputGroup, FormGroup, ControlLabel, FormControl, HelpBlock, DropdownButton, MenuItem,
} from 'react-bootstrap';
import lodash from 'lodash';

import { InputWrapper } from 'components/bootstrap';
import FormsUtils from 'util/FormsUtils';

/**
 * Component that renders a form field for a time unit value. The field has
 * a checkbox that enables/disables the input, a input for the time value,
 * and a select that let the user choose the unit used for the given time
 * value.
 */
const TimeUnitInput = createReactClass({
  displayName: 'TimeUnitInput',

  propTypes: {
    /**
     * Function that will be called when the input changes, that is,
     * when the field is enabled/disabled, the value or the unit change.
     * The function will receive the value, unit, and checked boolean as
     * arguments.
     */
    update: PropTypes.func.isRequired,
    /** Label to use for the field. */
    label: PropTypes.string,
    /** Help message to use for the field. */
    help: PropTypes.string,
    /** Specifies if this is a required field or not. */
    required: PropTypes.bool,
    /** Specifies if the input is enabled or disabled. */
    enabled: PropTypes.bool,
    /** Specifies the value of the input. */
    value: PropTypes.number,
    /** Indicates the default value to use, in case value is not provided or set. */
    defaultValue: PropTypes.number,
    /** Indicates which unit is used for the value. */
    unit: PropTypes.oneOf(['NANOSECONDS', 'MICROSECONDS', 'MILLISECONDS', 'SECONDS', 'MINUTES', 'HOURS', 'DAYS']),
    /** Add an additional class to the label. */
    labelClassName: PropTypes.string,
    /** Add an additional class to the input wrapper. */
    wrapperClassName: PropTypes.string,
  },

  getDefaultProps() {
    return {
      defaultValue: 1,
      value: undefined,
      unit: 'SECONDS',
      label: '',
      help: '',
      required: false,
      enabled: false,
      labelClassName: undefined,
      wrapperClassName: undefined,
    };
  },

  OPTIONS: [
    { value: 'NANOSECONDS', label: 'nanoseconds' },
    { value: 'MICROSECONDS', label: 'microseconds' },
    { value: 'MILLISECONDS', label: 'milliseconds' },
    { value: 'SECONDS', label: 'seconds' },
    { value: 'MINUTES', label: 'minutes' },
    { value: 'HOURS', label: 'hours' },
    { value: 'DAYS', label: 'days' },
  ],

  _getEffectiveValue() {
    return lodash.defaultTo(this.props.value, this.props.defaultValue);
  },

  _isChecked() {
    return this.props.required || this.props.enabled;
  },

  _propagateInput(update) {
    const previousInput = {
      value: this._getEffectiveValue(),
      unit: this.props.unit,
      checked: this._isChecked(),
    };
    const nextInput = Object.assign({}, previousInput, update);
    this.props.update(nextInput.value, nextInput.unit, nextInput.checked);
  },

  _onToggleEnable(e) {
    this._propagateInput({ checked: e.target.checked });
  },

  _onUpdate(e) {
    const value = lodash.defaultTo(FormsUtils.getValueFromInput(e.target), this.props.defaultValue);
    this._propagateInput({ value: value });
  },

  _onUnitSelect(unit) {
    this._propagateInput({ unit: unit });
  },

  render() {
    const options = this.OPTIONS.map((o) => {
      return <MenuItem key={o.value} onSelect={() => this._onUnitSelect(o.value)}>{o.label}</MenuItem>;
    });

    const checkbox = (<InputGroup.Addon>
      <input type="checkbox" checked={this._isChecked()} onChange={this._onToggleEnable} />
    </InputGroup.Addon>);

    return (
      <FormGroup>
        {this.props.label && <ControlLabel className={this.props.labelClassName}>{this.props.label}</ControlLabel>}
        <InputWrapper className={this.props.wrapperClassName}>
          <InputGroup>
            {!this.props.required && checkbox}
            <FormControl type="number" disabled={!this._isChecked()} onChange={this._onUpdate} value={this._getEffectiveValue()} />
            <DropdownButton componentClass={InputGroup.Button}
                            id="input-dropdown-addon"
                            title={this.OPTIONS.filter(o => o.value === this.props.unit)[0].label}>
              {options}
            </DropdownButton>
          </InputGroup>
          {this.props.help && <HelpBlock>{this.props.help}</HelpBlock>}
        </InputWrapper>
      </FormGroup>
    );
  },
});

export default TimeUnitInput;
