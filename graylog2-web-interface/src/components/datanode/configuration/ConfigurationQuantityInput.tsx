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

import type { ConfigurationQuantityKind } from './types';
import { formatQuantityValue, getQuantityUnits, parseQuantityValue } from './configurationQuantities';

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
  kind: ConfigurationQuantityKind;
  value: string;
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
  kind,
  value,
  disabled,
  invalid = false,
  describedBy = undefined,
  labelledBy = undefined,
  onChange,
  onBlur,
}: Props) => {
  const { amount, unit } = parseQuantityValue(value, kind);
  const units = getQuantityUnits(kind);
  const maxByUnit: Record<string, number> = { '%': 100, ratio: 1 };
  const selectedUnit = units.find((option) => option.value === unit);

  return (
    <QuantityInputGroup>
      <QuantityControl
        id={id}
        name={id}
        type="number"
        value={amount}
        placeholder="No override set"
        min={kind === 'heap' ? 1 : 0}
        max={maxByUnit[unit]}
        step={kind === 'heap' ? 1 : 'any'}
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
        title={selectedUnit?.label}
        disabled={disabled}
        onBlur={onBlur}>
        {units.map((option) => (
          <MenuItem
            key={option.value}
            active={unit === option.value}
            onSelect={() => onChange(formatQuantityValue(amount, option.value))}>
            {option.label}
          </MenuItem>
        ))}
      </DropdownButton>
    </QuantityInputGroup>
  );
};

export default ConfigurationQuantityInput;
