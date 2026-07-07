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
import styled from 'styled-components';

const Wrapper = styled.div`
  position: relative;
  display: block;
  padding-top: 7px;
`;

const StyledLabel = styled.label`
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 20px;
  font-weight: normal;
  cursor: pointer;
  margin-bottom: 0;

  input[type='radio'] {
    margin-top: 1px;
  }
`;

type Props = React.PropsWithChildren<{
  'aria-describedby'?: string;
  'aria-label'?: string;
  'aria-labelledby'?: string;
  checked?: boolean;
  className?: string;
  defaultChecked?: boolean;
  disabled?: boolean;
  id?: string;
  name?: string;
  onChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
  title?: string;
  value?: string | number;
}>;

const Radio = (
  {
    'aria-describedby': ariaDescribedBy = undefined,
    'aria-label': ariaLabel = undefined,
    'aria-labelledby': ariaLabelledBy = undefined,
    checked = undefined,
    children = undefined,
    className = undefined,
    defaultChecked = undefined,
    disabled = false,
    id = undefined,
    name = undefined,
    onChange,
    title = undefined,
    value = undefined,
  }: Props,
  ref: React.Ref<HTMLInputElement>,
) => (
  <Wrapper className={className}>
    <StyledLabel htmlFor={id} title={title}>
      <input
        ref={ref}
        type="radio"
        id={id}
        name={name}
        value={value}
        checked={checked}
        defaultChecked={defaultChecked}
        disabled={disabled}
        aria-describedby={ariaDescribedBy}
        aria-label={ariaLabel}
        aria-labelledby={ariaLabelledBy}
        onChange={onChange}
      />
      {children}
    </StyledLabel>
  </Wrapper>
);

export default React.forwardRef(Radio);
