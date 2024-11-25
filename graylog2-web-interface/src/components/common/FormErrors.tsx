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
import isEmpty from 'lodash/isEmpty';

const Errors = styled.div(({ theme }) => css`
  width: 100%;
  margin-top: 3px;
  color: ${theme.colors.variant.danger};

  > * {
    margin-right: 5px;

    &:last-child {
      margin-right: 0;
    }
  }
`);

const FormErrors = ({ errors = {} }: { errors: { [name: string]: string }}) => {
  if (isEmpty(errors) || Object.values(errors).every((v) => !v)) return null;

  return (
    <Errors>
      {Object.entries(errors).map(([fieldKey, value]: [string, unknown]) => (
        <span key={fieldKey}>{String(value)}.</span>
      ))}
    </Errors>
  );
};

export default FormErrors;
