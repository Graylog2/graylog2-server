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

import { Input } from 'components/bootstrap';
import formValidation from 'integrations/aws/utils/formValidation';
import Icon from 'components/common/Icon';

const Label = ({ label, error }) => {
  if (error) {
    return (
      <ErrorContainer>
        {label}
        <Error><Icon name="warning" /> {error}</Error>
      </ErrorContainer>
    );
  }

  return label;
};

type ValidatedInputProps = {
  className?: string;
  fieldData?: {
    error?: string;
    dirty?: boolean;
    value?: string;
  };
  help?: string;
  label: string | React.ReactNode;
  id: string;
  onChange?: (...args: any[]) => void;
  required?: boolean;
  type?: string;
} & React.ComponentProps<typeof Input>;

const ValidatedInput = ({
  className,
  help = '',
  onChange = () => {},
  id,
  label,

  fieldData = {
    dirty: false,
    error: undefined,
    value: undefined,
  },

  type,
  required = false,
  ...restProps
}: ValidatedInputProps) => {
  const { dirty, error, value } = fieldData;

  const checkValidity = (event) => {
    if (dirty) {
      const errorOutput = formValidation.checkInputValidity(event.target);

      onChange(event, { error: errorOutput });
    }
  };

  return (
    <Input {...restProps}
           required={required}
           id={id}
           type={type}
           onChange={onChange}
           onBlur={checkValidity}
           className={className}
           bsStyle={(error && dirty && 'error') || undefined}
           defaultValue={(type !== 'select' && value) || undefined}
           value={(type === 'select' && value) || undefined}
           label={<Label label={label} error={error} />}
           help={help} />
  );
};

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

export default ValidatedInput;
