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
import React, { useState, useEffect } from 'react';
import last from 'lodash/last';
import isInteger from 'lodash/isInteger';
import moment from 'moment';
import styled from 'styled-components';

import {
  ControlLabel,
  InputWrapper,
  FormControl,
  DropdownButton,
  HelpBlock,
  FormGroup,
  MenuItem,
  InputGroup,
} from 'components/bootstrap';
import { getValueFromInput } from 'util/FormsUtils';

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
  const timeUnit =
    timeUnits.find((unit) => {
      const durationInUnit = momentDuration.as(unit);

      return isInteger(durationInUnit) && durationInUnit !== 0;
    }) || last(timeUnits);
  const durationInUnit = momentDuration.as(timeUnit);

  return {
    duration: durationInUnit,
    unit: timeUnit,
  };
};

const StyledInputGroup = styled(InputGroup)`
  display: flex;
`;

const InputGroupAddon = styled(InputGroup.Addon)`
  display: flex;
  justify-content: center;
  align-items: center;
`;

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

type UnitValue = (typeof unitValues)[number];

const defaultUnits: UnitValue[] = [
  'NANOSECONDS',
  'MICROSECONDS',
  'MILLISECONDS',
  'SECONDS',
  'MINUTES',
  'HOURS',
  'DAYS',
];

type UnitOption = {
  value: UnitValue;
  label: string;
};

type Props = {
  update: (value: number, unit: string, checked: boolean) => void;
  value?: number | string;
  enabled?: boolean;
  defaultEnabled?: boolean;
  disabled?: boolean;
  label?: string;
  name?: string;
  help?: React.ReactNode;
  required?: boolean;
  clearable?: boolean;
  defaultValue?: number;
  pullRight?: boolean;
  hideCheckbox?: boolean;
  unit?: string;
  units?: string[];
  unitName?: string;
};

const TimeUnitInput = ({
  update,
  value = undefined,
  unit = 'SECONDS',
  enabled = undefined,
  defaultEnabled = false,
  disabled = false,
  label = '',
  help = '',
  name = null,
  required = false,
  clearable = false,
  defaultValue = 1,
  pullRight = false,
  hideCheckbox = false,
  units = defaultUnits,
  unitName = null,
}: Props) => {
  const [unitOptions, setUnitOptions] = useState<UnitOption[]>([]);
  const [checked, setChecked] = useState<boolean>(enabled ?? defaultEnabled);

  const getEffectiveValue = () => (clearable ? value : (value ?? defaultValue));

  const getCheckedValue = () => required || checked;

  const propagateChange = (change) => {
    const prevValue = {
      value: getEffectiveValue(),
      unit: unit,
      checked: getCheckedValue(),
    };
    const nextValue = {
      ...prevValue,
      ...change,
    };
    update(nextValue.value, nextValue.unit, nextValue.checked);
  };

  const handleValueChange = (e) => {
    if (clearable) propagateChange({ value: getValueFromInput(e.target) });
    else propagateChange({ value: getValueFromInput(e.target) ?? defaultValue });
  };

  const handleCheckedChange = (e) => {
    setChecked(e.target.checked);
    propagateChange({ checked: e.target.checked });
  };

  useEffect(() => {
    const parseUnitOptions = () => {
      setUnitOptions(
        unitValues
          .filter((option) => units.includes(option))
          .map((option) => ({ value: option, label: option.toLowerCase() })),
      );
    };

    parseUnitOptions();
  }, [units]);

  return (
    <FormGroup>
      {label && <ControlLabel>{label}</ControlLabel>}
      <InputWrapper>
        <StyledInputGroup>
          {!required && !hideCheckbox && !disabled && (
            <InputGroupAddon>
              <input type="checkbox" checked={getCheckedValue()} onChange={handleCheckedChange} title="Toggle time" />
            </InputGroupAddon>
          )}
          <FormControl
            type="number"
            name={name}
            disabled={!getCheckedValue() || disabled}
            aria-label={label || 'Time unit input'}
            onChange={(e) => handleValueChange(e)}
            value={getEffectiveValue() ?? ''}
          />
          <DropdownButton
            id="input-dropdown-addon"
            name={unitName}
            pullRight={pullRight}
            title={unitOptions.filter((option: UnitOption) => option.value === unit)[0]?.label}
            disabled={!getCheckedValue() || disabled}>
            {unitOptions.map((option: UnitOption) => (
              <MenuItem
                key={option.value}
                onSelect={() => propagateChange({ unit: option.value })}
                active={unit === option.value}>
                {option.label}
              </MenuItem>
            ))}
          </DropdownButton>
        </StyledInputGroup>
        {help && <HelpBlock>{help}</HelpBlock>}
      </InputWrapper>
    </FormGroup>
  );
};

export default TimeUnitInput;
