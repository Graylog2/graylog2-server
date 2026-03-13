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
import * as React from 'react';
import { useCallback } from 'react';
import { useField } from 'formik';
import styled from 'styled-components';

import { Select } from 'components/common';
import { ControlLabel } from 'components/bootstrap';

const Col = styled.div`
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  width: 100%;
  align-items: stretch;
  justify-content: flex-start;
`;

const ErrorMessage = styled.small`
  color: ${({ theme }) => theme.colors.variant.danger};
`;

type Props<T> = {
  field: any;
  options: { label: string; value: T }[];
  onChange: (newValue: string) => void;
  label?: string;
  disabled?: boolean;
  clearable?: boolean;
  menuPlacement?: 'bottom' | 'auto' | 'top';
  multi?: boolean;
};

function SelectFormik<T>({
  field,
  options,
  onChange,
  label = undefined,
  disabled = false,
  clearable = true,
  menuPlacement = 'bottom',
  multi = false,
}: Props<T>) {
  const [{ name, value }, { touched, error }, { setValue, setTouched }] = useField({ name: field.name });

  const handleChange = useCallback(
    (newValue: string) => {
      setValue(multi ? newValue.split(',') : newValue);
      onChange?.(newValue);
    },
    [setValue, onChange, multi],
  );

  return (
    <Col>
      {label && <ControlLabel>{label}</ControlLabel>}
      <Select
        id={name}
        inputProps={{ 'aria-label': name }}
        name={name}
        multi={multi}
        onChange={handleChange}
        onBlur={() => setTouched(true)}
        value={Array.isArray(value) ? value.join(',') : value}
        disabled={disabled}
        options={options}
        menuPlacement={menuPlacement}
        clearable={clearable}
      />
      {touched && error && <ErrorMessage>{error}</ErrorMessage>}
    </Col>
  );
}

export default SelectFormik;
