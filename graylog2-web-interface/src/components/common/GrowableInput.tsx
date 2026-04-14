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
import styled, { css } from 'styled-components';

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

const Wrapper = styled.div(
  ({ theme }) => css`
    width: 100%;

    .growable-input-wrapper {
      display: grid;
      width: 100%;
      max-width: 100%;
      min-width: 0;
      grid-template-columns: minmax(0, 1fr);
    }

    .growable-input-wrapper::after,
    .growable-input-wrapper > textarea {
      grid-area: 1 / 1 / 2 / 2;
    }

    .growable-input-wrapper::after {
      content: attr(data-input-value) ' ';
      white-space: pre-wrap;
      overflow-wrap: break-word;
      visibility: hidden;
      box-sizing: border-box;
      border: 1px solid transparent;
      padding: 6px 12px;
      font-size: ${theme.fonts.size.body};
      line-height: inherit;
    }

    .growable-input-wrapper > textarea {
      resize: none;
      overflow: hidden;
      width: 100%;
      max-width: 100%;
      min-width: 0;
    }
  `,
);

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
    <Wrapper>
      <Input
        bsStyle={bsStyle}
        className={className}
        error={error}
        id={id}
        label={label}
        name={name}
        onBlur={onBlur}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
        readOnly={readOnly}
        required={required}
        rows={1}
        title={title}
        type="textarea"
        value={value}
        wrapperAttributes={{ 'data-input-value': value }}
        wrapperClassName="growable-input-wrapper"
      />
    </Wrapper>
  );
};

export default GrowableInput;
