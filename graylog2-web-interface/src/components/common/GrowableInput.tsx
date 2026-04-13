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
import React, { useLayoutEffect, useRef } from 'react';
import styled from 'styled-components';

import { Input } from 'components/bootstrap';

type Props = {
  bsStyle?: 'success' | 'warning' | 'error';
  className?: string;
  error?: React.ReactElement | string;
  id: string;
  label?: React.ReactElement | string;
  name: string;
  onBlur: React.FocusEventHandler<HTMLInputElement>;
  onChange: React.ChangeEventHandler<HTMLInputElement>;
  readOnly?: boolean;
  restrictLineBreaks?: boolean;
  required?: boolean;
  title: string;
  value?: string;
};

const StyledInput = styled(Input)`
  overflow: hidden;
  resize: none;
`;

// Behaves and looks like an input, but it grows with its content.
const GrowableInput = ({
  bsStyle = undefined,
  className = undefined,
  error = undefined,
  id,
  label = undefined,
  name,
  onBlur,
  onChange,
  readOnly = false,
  required = false,
  title,
  value = '',
  restrictLineBreaks = true,
}: Props) => {
  const inputRef = useRef<Input | null>(null);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const resizeTextarea = () => {
    const element = inputRef.current?.getInputDOMNode();

    if (!(element instanceof HTMLTextAreaElement)) {
      return;
    }

    element.style.height = 'auto';
    element.style.height = `${element.scrollHeight}px`;
  };

  useLayoutEffect(() => {
    resizeTextarea();
  }, [resizeTextarea, value]);

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const sanitizedValue = restrictLineBreaks ? event.target.value.replace(/[\r\n]+/g, ' ') : event.target.value;

    onChange({
      target: { name: event.target.name, value: sanitizedValue },
    } as React.ChangeEvent<HTMLInputElement>);
  };

  const handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    if (restrictLineBreaks && event.key === 'Enter') {
      event.preventDefault();
    }
  };

  return (
    <StyledInput
      bsStyle={bsStyle}
      className={className}
      error={error}
      id={id}
      label={label}
      name={name}
      onBlur={onBlur}
      onChange={handleChange}
      onKeyDown={handleKeyDown}
      ref={inputRef}
      readOnly={readOnly}
      required={required}
      rows={1}
      title={title}
      type="textarea"
      value={value}
    />
  );
};

export default GrowableInput;
