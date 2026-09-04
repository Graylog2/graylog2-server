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
import { useContext } from 'react';
import styled, { css } from 'styled-components';
import type { CSSProperties } from 'react';

import { FormGroupControlIdContext } from './FormGroup';

const StyledLabel = styled.label(
  ({ theme }) => css`
    color: ${theme.colors.text.primary};
    font-weight: bold;
    margin-bottom: 5px;
    display: inline-block;
  `,
);

type Props = {
  children: React.ReactNode;
  className?: string;
  htmlFor?: string;
  style?: CSSProperties;
};

const ControlLabel = ({ children, className = undefined, htmlFor = undefined, style = undefined }: Props) => {
  const controlId = useContext(FormGroupControlIdContext);

  return (
    <StyledLabel htmlFor={htmlFor ?? controlId} className={`${className ?? ''} control-label`} style={style}>
      {children}
    </StyledLabel>
  );
};

/** @component */
export default ControlLabel;
