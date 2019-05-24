import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import {
  ControlLabel,
  DropdownButton,
  FormControl,
  FormGroup,
  HelpBlock,
  InputGroup,
  MenuItem,
} from 'react-bootstrap';
import lodash from 'lodash';

import { InputWrapper } from 'components/bootstrap';
import FormsUtils from 'util/FormsUtils';

const unitValues = [
  'NANOSECONDS',
  'MICROSECONDS',
  'MILLISECONDS',
  'SECONDS',
  'MINUTES',
  'HOURS',
  'DAYS',
];
const unitType = PropTypes.oneOf(unitValues);

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
    /** Indicates the default enabled state, in case the consumer does not want to handle the enabled state. */
    defaultEnabled: PropTypes.bool,
    /** Specifies the value of the input. */
    value: PropTypes.number,
    /** Indicates the default value to use, in case value is not provided or set. */
    defaultValue: PropTypes.number,
    /** Indicates which unit is used for the value. */
    unit: unitType,
    /** Specifies which units should be available in the form. */
    units: PropTypes.arrayOf(unitType),
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
      units: unitValues,
      label: '',
      help: '',
      required: false,
      enabled: undefined,
      defaultEnabled: false,
      labelClassName: undefined,
      wrapperClassName: undefined,
    };
  },

  getInitialState() {
    const { defaultEnabled, enabled, units } = this.props;
    return {
      enabled: lodash.defaultTo(enabled, defaultEnabled),
      unitOptions: this._getUnitOptions(units),
    };
  },

  componentWillReceiveProps(nextProps) {
    const { units } = this.props;
    if (!lodash.isEqual(units, nextProps.units)) {
      this.setState({ unitOptions: this._getUnitOptions(nextProps.units) });
    }
  },

  _getEffectiveValue() {
    const { defaultValue, value } = this.props;
    return lodash.defaultTo(value, defaultValue);
  },

  _getUnitOptions(units) {
    return unitValues
      .filter(value => units.includes(value))
      .map(value => ({ value: value, label: value.toLowerCase() }));
  },

  _isChecked() {
    const { required, enabled } = this.props;
    if (required) {
      return required;
    }
    const { enabled: enabledState } = this.state;
    return lodash.defaultTo(enabled, enabledState);
  },

  _propagateInput(update) {
    const { update: onUpdate, unit } = this.props;
    const previousInput = {
      value: this._getEffectiveValue(),
      unit: unit,
      checked: this._isChecked(),
    };
    const nextInput = Object.assign({}, previousInput, update);
    onUpdate(nextInput.value, nextInput.unit, nextInput.checked);
  },

  _onToggleEnable(e) {
    const isChecked = e.target.checked;
    this.setState({ enabled: isChecked });
    this._propagateInput({ checked: isChecked });
  },

  _onUpdate(e) {
    const value = lodash.defaultTo(FormsUtils.getValueFromInput(e.target), this.props.defaultValue);
    this._propagateInput({ value: value });
  },

  _onUnitSelect(unit) {
    this._propagateInput({ unit: unit });
  },

  render() {
    const { unitOptions } = this.state;
    const options = unitOptions.map((o) => {
      return <MenuItem key={o.value} onSelect={() => this._onUnitSelect(o.value)}>{o.label}</MenuItem>;
    });

    const checkbox = (
      <InputGroup.Addon>
        <input type="checkbox" checked={this._isChecked()} onChange={this._onToggleEnable} />
      </InputGroup.Addon>
    );

    const { label, wrapperClassName, help, labelClassName, unit, required } = this.props;
    return (
      <FormGroup>
        {label && <ControlLabel className={labelClassName}>{label}</ControlLabel>}
        <InputWrapper className={wrapperClassName}>
          <InputGroup>
            {!required && checkbox}
            <FormControl type="number" disabled={!this._isChecked()} onChange={this._onUpdate} value={this._getEffectiveValue()} />
            <DropdownButton componentClass={InputGroup.Button}
                            id="input-dropdown-addon"
                            title={unitOptions.filter(o => o.value === unit)[0].label}
                            disabled={!this._isChecked()}>
              {options}
            </DropdownButton>
          </InputGroup>
          {help && <HelpBlock>{help}</HelpBlock>}
        </InputWrapper>
      </FormGroup>
    );
  },
});

export default TimeUnitInput;
