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
import styled from 'styled-components';

import { DropdownButton, FormControl, InputGroup, MenuItem } from 'components/bootstrap';

import type { ParameterValues } from './types';
import { formatQuantityValue, parseQuantityValue } from './configurationQuantities';

// Follow the attached numeric field and unit dropdown pattern used by TimeUnitInput.
const QuantityInputGroup = styled(InputGroup)`
  display: flex;
`;

const QuantityControl = styled(FormControl)`
  flex: 1;
  min-width: 0;
`;

type Props = {
  id: string;
  label: string;
  units: ParameterValues;
  value: string | undefined;
  disabled: boolean;
  invalid?: boolean;
  describedBy?: string;
  labelledBy?: string;
  onChange: (value: string) => void;
  onBlur: () => void;
};

const ConfigurationQuantityInput = ({
  id,
  label,
  units,
  value,
  disabled,
  invalid = false,
  describedBy = undefined,
  labelledBy = undefined,
  onChange,
  onBlur,
}: Props) => {
  const { amount, unit } = parseQuantityValue(value, units);
  // A percentage cannot exceed 100, and a unitless quantity is a ratio from 0 to 1
  const maxByUnit: Record<string, number> = { '%': 100, '': 1 };
  const unitOptions = Object.entries(units);

  return (
    <QuantityInputGroup>
      <QuantityControl
        id={id}
        name={id}
        type="number"
        value={amount}
        placeholder="No value set"
        min={0}
        max={maxByUnit[unit]}
        step="any"
        aria-invalid={invalid}
        aria-describedby={describedBy}
        aria-labelledby={labelledBy}
        disabled={disabled}
        onChange={(event) => onChange(formatQuantityValue((event.target as HTMLInputElement).value, unit))}
        onBlur={onBlur}
      />
      <DropdownButton
        id={`${id}-unit`}
        buttonTitle={`${label} unit`}
        title={units[unit]}
        disabled={disabled}
        onBlur={onBlur}>
        {unitOptions.map(([suffix, unitLabel]) => (
          <MenuItem
            key={suffix}
            active={unit === suffix}
            onSelect={() => onChange(formatQuantityValue(amount, suffix))}>
            {unitLabel}
          </MenuItem>
        ))}
      </DropdownButton>
    </QuantityInputGroup>
  );
};

export default ConfigurationQuantityInput;
