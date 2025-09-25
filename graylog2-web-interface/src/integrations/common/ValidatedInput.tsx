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

import { Icon } from 'components/common';
import { Input } from 'components/bootstrap';

import type { FormFieldDataType, HandleFieldUpdateType } from './utils/types';

type LabelProps = { label: string; error: string };

const Error = styled.span`
  display: block;
  font-weight: normal;
  padding-left: 15px;
  font-size: 0.85em;
`;

const ErrorContainer = styled.span`
  display: flex;
  align-items: center;
`;

const Label = ({ label, error }: LabelProps) => {
  if (error) {
    return (
      <ErrorContainer>
        {label}
        <Error>
          <Icon name="warning" />
          {error}
        </Error>
      </ErrorContainer>
    );
  }

  return <> {label}</>;
};

type ValidatedInputProps = React.ComponentProps<typeof Input> & {
  className?: string;
  fieldData?: FormFieldDataType;
  help?: string;
  label: string;
  id: string;
  onChange?: HandleFieldUpdateType;
  required?: boolean;
  type: string;
  placeholder?: string;
  autoComplete?: string;
  defaultValue?: any;
};

const ValidatedInput = ({
  id,
  label,
  type,
  className = undefined,
  onChange = () => {},
  help = '',
  fieldData = {
    dirty: false,
    error: undefined,
    value: undefined,
  },
  ...restProps
}: React.PropsWithChildren<ValidatedInputProps>) => {
  const { dirty, error, value } = fieldData;

  const checkValidity = (event: React.FocusEvent<HTMLInputElement>) => {
    if (dirty) {
      const errorOutput = '';
      onChange(event, { error: errorOutput });
    }
  };

  if (type === 'file') {
    return (
      <Input
        {...restProps}
        id={id}
        type={type}
        onChange={onChange}
        onBlur={checkValidity}
        className={className}
        bsStyle={(error && dirty && 'error') || undefined}
        label={<Label label={label} error={error} />}
        help={help}
      />
    );
  }

  return (
    <Input
      {...restProps}
      id={id}
      type={type}
      onChange={onChange}
      onBlur={checkValidity}
      className={className}
      bsStyle={(error && dirty && 'error') || undefined}
      defaultValue={(type !== 'select' && type !== 'file' && value) || undefined}
      value={(type === 'select' && value) || undefined}
      label={<Label label={label} error={error} />}
      help={help}
    />
  );
};

export default ValidatedInput;
