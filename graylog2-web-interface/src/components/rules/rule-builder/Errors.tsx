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

import { Col, Row } from 'components/bootstrap';

import type { ObjectWithErrors } from './types';

type Props = {
  objectWithErrors?: ObjectWithErrors
}

const ErrorsContainer = styled(Row)(({ theme }) => css`
  margin-top: ${theme.spacings.xs};
`);

const Error = styled.p(({ theme }) => css`
  color: ${theme.colors.variant.danger};
  margin-top: ${theme.spacings.xs};
  margin-bottom: ${theme.spacings.xs};
`);

const Errors = ({ objectWithErrors } : Props) => {
  if (!objectWithErrors) { return null; }
  if (!objectWithErrors.errors) { return null; }
  if (!(objectWithErrors.errors.length > 0)) { return null; }

  return (
    <ErrorsContainer>
      <Col md={12}>
        {objectWithErrors?.errors?.map((error) => (
          <Row key={error}>
            <Col md={12}>
              <Error>{error}</Error>
            </Col>
          </Row>
        ))}
      </Col>
    </ErrorsContainer>
  );
};

export default Errors;
