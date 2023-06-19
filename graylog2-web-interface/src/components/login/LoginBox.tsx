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

const Wrapper = styled.div(({ theme }) => css`
  background-color: ${theme.colors.global.contentBackground};
  display: flex;
  justify-content: center;
  align-items: center;
  width: 33%;
  padding: 30px;
  max-width: 440px;
  min-width: 330px;
`);

const Container = styled.div`
  width: 100%;
`;

type Props = {
  children: React.ReactNode,
};

const LoginBox = ({ children }: Props) => (
  <Wrapper className="container">
    <Container>
      {children}
    </Container>
  </Wrapper>
);

LoginBox.propTypes = {
  children: PropTypes.node.isRequired,
};

export default LoginBox;
