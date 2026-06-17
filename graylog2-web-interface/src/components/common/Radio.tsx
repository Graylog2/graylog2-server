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
import styled, { css } from 'styled-components';

const Wrapper = styled.div(
  ({ theme }) => css`
    position: relative;
    display: block;
    margin-top: ${theme.spacings.sm};
    margin-bottom: ${theme.spacings.sm};
  `,
);

const StyledLabel = styled.label`
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 20px;
  font-weight: normal;
  cursor: pointer;
  margin-bottom: 0;

  &:has(input:disabled) {
    cursor: not-allowed;
    opacity: 0.65;
  }
`;

type Props = React.PropsWithChildren<{
  checked: boolean;
  className?: string;
  disabled?: boolean;
  id?: string;
  name?: string;
  onChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
  value?: string | number;
}>;

const Radio = React.forwardRef<HTMLInputElement, Props>(
  (
    {
      checked,
      children = undefined,
      className = undefined,
      disabled = false,
      id = undefined,
      name = undefined,
      onChange,
      value = undefined,
    },
    ref,
  ) => (
    <Wrapper className={className}>
      <StyledLabel htmlFor={id}>
        <input
          ref={ref}
          type="radio"
          id={id}
          name={name}
          value={value}
          checked={checked}
          disabled={disabled}
          onChange={onChange}
        />
        {children}
      </StyledLabel>
    </Wrapper>
  ),
);

Radio.displayName = 'Radio';

export default Radio;
