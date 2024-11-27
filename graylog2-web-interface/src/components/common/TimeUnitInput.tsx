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
import defaultTo from 'lodash/defaultTo';
import isEqual from 'lodash/isEqual';
import last from 'lodash/last';
import isInteger from 'lodash/isInteger';
import moment from 'moment';
import styled from 'styled-components';

import {
  ControlLabel,
  DropdownButton,
  FormControl,
  FormGroup,
  HelpBlock,
  InputGroup,
  InputWrapper,
  MenuItem,
} from 'components/bootstrap';
import * as FormsUtils from 'util/FormsUtils';

const unitValues = [
  'NANOSECONDS',
  'MICROSECONDS',
  'MILLISECONDS',
  'SECONDS',
  'MINUTES',
  'HOURS',
  'DAYS',
  'MONTHS',
  'YEARS',
] as const;
type UnitValue = typeof unitValues[number];
const defaultUnits = [
  'NANOSECONDS',
  'MICROSECONDS',
  'MILLISECONDS',
  'SECONDS',
  'MINUTES',
  'HOURS',
  'DAYS',
];

const StyledInputGroup = styled(InputGroup)`
  display: flex;
`;

const InputGroupAddon = styled(InputGroup.Addon)`
  display: flex;
  justify-content: center;
  align-items: center;
`;

/**
 * Returns a duration and unit compatible with `TimeUnitInput` from a duration accepted by `moment.duration()`
 * and a list of time units.
 *
 * Accepted durations include a number of milliseconds and an ISO 8601 duration string.
 */
export const extractDurationAndUnit = (duration, timeUnits) => {
  if (duration === undefined) {
    return {
      duration: 1,
      unit: last(timeUnits),
    };
  }

  const momentDuration = moment.duration(duration);
  const timeUnit = timeUnits.find((unit) => {
    const durationInUnit = momentDuration.as(unit);

    return isInteger(durationInUnit) && durationInUnit !== 0;
  }) || last(timeUnits);
  const durationInUnit = momentDuration.as(timeUnit);

  return {
    duration: durationInUnit,
    unit: timeUnit,
  };
};

/**
 * Component that renders a form field for a time unit value. The field has
 * a checkbox that enables/disables the input, a input for the time value,
 * and a select that let the user choose the unit used for the given time
 * value.
 */
type Props = {
  /**
   * Function that will be called when the input changes, that is,
   * when the field is enabled/disabled, the value or the unit change.
   * The function will receive the value, unit, and checked boolean as
   * arguments.
   */
  update: (value: number, unit: string, checked: boolean) => void,
  /** Label to use for the field. */
  label?: string,
  /** Help message to use for the field. */
  help?: React.ReactNode,
  /** Specifies if this is a required field or not. */
  required?: boolean,
  /** Specifies if the input is enabled or disabled. */
  enabled?: boolean,
  /** Indicates the default enabled state, in case the consumer does not want to handle the enabled state. */
  defaultEnabled?: boolean,
  /** Specifies the value of the input. */
  value?: number | string,
  /** Indicates the default value to use, in case value is not provided or set. */
  defaultValue?: number,
  /** Indicates which unit is used for the value. */
  unit?: string,
  /** Specifies which units should be available in the form. */
  units?: Array<string>,
  /** Add an additional class to the label. */
  labelClassName?: string,
  /** Add an additional class to the input wrapper. */
  wrapperClassName?: string,
  /** Specifies if the input should render a checkbox. Use this if the enabled state is controlled by another input */
  hideCheckbox?: boolean,
  /** Align unit dropdown menu to the right. */
  pullRight?: boolean,
  /** Lets the user clear the numeric input. */
  clearable?: boolean,

  name?: string,
  unitName?: string,
  // TODO: Added to avoid messing with existing code, should be considered for removal
  id?: string,
  // TODO: Added to avoid messing with existing code, should be considered for removal
  type?: string,
}

type State = {
  enabled: boolean,
  unitOptions: Array<{ label: string, value: UnitValue }>
}

class TimeUnitInput extends React.Component<Props, State> {
  static defaultProps = {
    defaultValue: 1,
    value: undefined,
    unit: 'SECONDS',
    units: defaultUnits,
    label: '',
    help: '',
    name: null,
    unitName: null,
    required: false,
    enabled: undefined,
    defaultEnabled: false,
    labelClassName: undefined,
    wrapperClassName: undefined,
    hideCheckbox: false,
    pullRight: false,
    clearable: false,
  };

  constructor(props) {
    super(props);

    const { defaultEnabled, enabled, units } = props;

    this.state = {
      enabled: defaultTo(enabled, defaultEnabled),
      unitOptions: this._getUnitOptions(units),
    };
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    const { units } = this.props;

    if (!isEqual(units, nextProps.units)) {
      this.setState({ unitOptions: this._getUnitOptions(nextProps.units) });
    }
  }

  _getEffectiveValue = () => {
    const { defaultValue, value, clearable } = this.props;

    return clearable ? value : defaultTo(value, defaultValue);
  };

  _getUnitOptions = (units) => unitValues
    .filter((value) => units.includes(value))
    .map((value) => ({ value: value, label: value.toLowerCase() }));

  _isChecked = () => {
    const { required, enabled } = this.props;

    if (required) {
      return required;
    }

    const { enabled: enabledState } = this.state;

    return defaultTo(enabled, enabledState);
  };

  _propagateInput = (update) => {
    const { update: onUpdate, unit } = this.props;
    const previousInput = {
      value: this._getEffectiveValue(),
      unit: unit,
      checked: this._isChecked(),
    };
    const nextInput = { ...previousInput, ...update };

    onUpdate(nextInput.value, nextInput.unit, nextInput.checked);
  };

  _onToggleEnable = (e) => {
    const isChecked = e.target.checked;

    this.setState({ enabled: isChecked });
    this._propagateInput({ checked: isChecked });
  };

  _onUpdate = (e) => {
    const { defaultValue, clearable } = this.props;
    let value;

    if (clearable) {
      value = FormsUtils.getValueFromInput(e.target);
    } else {
      value = defaultTo(FormsUtils.getValueFromInput(e.target), defaultValue);
    }

    this._propagateInput({ value: value });
  };

  _onUnitSelect = (unit) => {
    this._propagateInput({ unit: unit });
  };

  render() {
    const { unitOptions } = this.state;
    const { label, wrapperClassName, help, labelClassName, unit, required, hideCheckbox, pullRight } = this.props;

    const options = unitOptions.map((o) => (
      <MenuItem key={o.value}
                onSelect={() => this._onUnitSelect(o.value)}
                active={unit === o.value}>
        {o.label}
      </MenuItem>
    ));

    const checkbox = (
      <InputGroupAddon>
        <input type="checkbox" checked={this._isChecked()} onChange={this._onToggleEnable} title="Toggle time" />
      </InputGroupAddon>
    );

    return (
      <FormGroup>
        {label && <ControlLabel className={labelClassName}>{label}</ControlLabel>}
        <InputWrapper className={wrapperClassName}>
          <StyledInputGroup>
            {(!required && !hideCheckbox) && checkbox}
            <FormControl type="number"
                         name={this.props.name}
                         disabled={!this._isChecked()}
                         aria-label={label || 'Time unit input'}
                         onChange={this._onUpdate}
                         value={defaultTo(this._getEffectiveValue(), '')} />
            <DropdownButton id="input-dropdown-addon"
                            name={this.props.unitName}
                            pullRight={pullRight}
                            title={unitOptions.filter((o) => o.value === unit)[0].label}
                            disabled={!this._isChecked()}>
              {options}
            </DropdownButton>
          </StyledInputGroup>
          {help && <HelpBlock>{help}</HelpBlock>}
        </InputWrapper>
      </FormGroup>
    );
  }
}

export default TimeUnitInput;
