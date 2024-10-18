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
import type { MantineStyleProp } from '@mantine/core';
import { Switch as MantineSwitch } from '@mantine/core';
import styled, { css } from 'styled-components';

const StyledSwitch = styled(MantineSwitch)(({ theme }) => css`
  .mantine-Switch-label {
    font-weight: normal;
    font-size: ${theme.fonts.size.body};
  }
`);

type Props = {
  'aria-label'?: string,
  checked: boolean,
  className?: string,
  disabled?: boolean,
  id?: string,
  label?: string
  name?: string,
  onChange: (event: React.ChangeEvent<HTMLInputElement>) => void,
  style?: MantineStyleProp,
}

const Switch = ({
  'aria-label': ariaLabel,
  checked,
  className,
  disabled,
  id,
  label,
  name,
  onChange,
  style,
}: Props) => (
  <StyledSwitch aria-label={ariaLabel}
                className={className}
                checked={checked}
                disabled={disabled}
                id={id}
                label={label}
                name={name}
                onChange={onChange}
                style={style} />
);

export default Switch;
