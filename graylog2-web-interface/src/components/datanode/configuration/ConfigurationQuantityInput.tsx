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

import type { ConfigurationUnits } from './types';

const QuantityInputGroup = styled(InputGroup)`
  display: flex;
`;

const QuantityControl = styled(FormControl)`
  flex: 1;
  min-width: 0;
`;

const QuantityActions = styled(InputGroup.Button)`
  display: flex;
  width: auto;
`;

const maxByUnit: Record<string, number> = { '%': 100, '': 1 };

const UnitDropdown = styled(DropdownButton)`
  border-color: ${({ theme }) => theme.colors.input.border};
`;

type Props = {
  id: string;
  label: string;
  units: ConfigurationUnits;
  amount: string;
  unit: string;
  placeholder: string;
  disabled: boolean;
  invalid?: boolean;
  describedBy?: string;
  onAmountChange: (amount: string) => void;
  onUnitChange: (unit: string) => void;
  onBlur: () => void;
  buttonAfter?: React.ReactNode;
};

const ConfigurationQuantityInput = ({
  id,
  label,
  units,
  amount,
  unit,
  placeholder,
  disabled,
  invalid = false,
  describedBy = undefined,
  onAmountChange,
  onUnitChange,
  onBlur,
  buttonAfter = undefined,
}: Props) => (
  <QuantityInputGroup>
    <QuantityControl
      id={id}
      name={id}
      type="number"
      value={amount}
      placeholder={placeholder}
      min={0}
      max={maxByUnit[unit]}
      step="any"
      aria-invalid={invalid}
      aria-describedby={describedBy}
      disabled={disabled}
      onChange={(event) => onAmountChange((event.target as HTMLInputElement).value)}
      onBlur={onBlur}
    />
    <QuantityActions>
      <UnitDropdown
        id={`${id}-unit`}
        bsStyle="transparent"
        buttonTitle={`${label} unit: ${units[unit]}`}
        title={units[unit]}
        disabled={disabled}
        onBlur={onBlur}>
        {Object.entries(units).map(([suffix, unitLabel]) => (
          <MenuItem key={suffix} active={unit === suffix} onSelect={() => onUnitChange(suffix)}>
            {unitLabel}
          </MenuItem>
        ))}
      </UnitDropdown>
      {buttonAfter}
    </QuantityActions>
  </QuantityInputGroup>
);

export default ConfigurationQuantityInput;
