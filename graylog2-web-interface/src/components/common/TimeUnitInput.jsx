import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import {
  InputGroup, FormGroup, ControlLabel, FormControl, HelpBlock, DropdownButton, MenuItem
} from 'react-bootstrap';
import { InputWrapper } from 'components/bootstrap';

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
    /** Indicates which unit is used for the value. */
    unit: PropTypes.oneOf(['NANOSECONDS', 'MICROSECONDS', 'MILLISECONDS', 'SECONDS', 'MINUTES', 'HOURS', 'DAYS']),
    /** Add an additional class to the label. */
    labelClassName: PropTypes.string,
    /** Add an additional class to the input wrapper. */
    wrapperClassName: PropTypes.string,
  },

  getDefaultProps() {
    return {
      value: 1,
      unit: 'SECONDS',
      label: '',
      help: '',
      required: false,
      enabled: false,
      labelClassName: undefined,
      wrapperClassName: undefined,
    };
  },

  getInitialState() {
    return {
      checked: this.props.required || this.props.enabled,
      value: this.props.value,
      unit: this.props.unit,
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

  _propagateState() {
    this.props.update(this.state.value, this.state.unit, this.state.checked);
  },

  _onToggleEnable(e) {
    this.setState({ checked: e.target.checked }, this._propagateState);
  },

  _onUpdate(e) {
    const value = e.target.value;
    this.setState({ value: value }, this._propagateState);
  },

  _onUnitSelect(unit) {
    this.setState({ unit: unit }, this._propagateState);
  },

  render() {
    const options = this.OPTIONS.map((o) => {
      return <MenuItem key={o.value} onSelect={() => this._onUnitSelect(o.value)}>{o.label}</MenuItem>;
    });

    const checkbox = (<InputGroup.Addon>
      <input type="checkbox" checked={this.state.checked} onChange={this._onToggleEnable} />
    </InputGroup.Addon>);

    return (
      <FormGroup>
        {this.props.label && <ControlLabel className={this.props.labelClassName}>{this.props.label}</ControlLabel>}
        <InputWrapper className={this.props.wrapperClassName}>
          <InputGroup>
            {!this.props.required && checkbox}
            <FormControl type="text" disabled={!this.state.checked} onChange={this._onUpdate} value={this.state.value} />
            <DropdownButton componentClass={InputGroup.Button}
                            id="input-dropdown-addon"
                            title={this.OPTIONS.filter(o => o.value === this.state.unit)[0].label}>
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
