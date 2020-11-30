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
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import chroma from 'chroma-js';

import { Col, Jumbotron, Row } from 'components/graylog';

const H1 = styled.h1(({ theme }) => css`
  font-size: ${theme.fonts.size.huge};
  margin-bottom: 15px;
`);

const ContainerRow = styled(Row)`
  height: 82vh;
`;

const StyledErrorJumbotron = styled(Jumbotron)(({ theme }) => css`
  background-color: ${chroma(theme.colors.global.contentBackground).alpha(0.8).css()};
  text-align: center;
`);

const ErrorJumbotron = ({ children, title }) => {
  return (
    <ContainerRow>
      <Col mdOffset={2} md={8}>
        <StyledErrorJumbotron>
          <H1>{title}</H1>
          {children}
        </StyledErrorJumbotron>
      </Col>
    </ContainerRow>
  );
};

ErrorJumbotron.propTypes = {
  children: PropTypes.node.isRequired,
  title: PropTypes.string.isRequired,
};

export default ErrorJumbotron;
