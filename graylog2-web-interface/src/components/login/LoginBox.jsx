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

import { Col, Row } from 'components/graylog';

const LoginCol = styled(Col)(({ theme }) => css`
  padding: 15px;
  background-color: ${theme.colors.global.contentBackground};
  border: 1px solid ${theme.colors.variant.light.default};
  border-radius: 4px;
  box-shadow: 0 0 21px ${theme.colors.global.navigationBoxShadow};
  margin-top: 120px;
  
  legend {
    color: ${theme.colors.variant.darker.default};
    border-color: ${theme.colors.variant.dark.default};
  }
`);

const LoginBox = ({ children }) => {
  return (
    <div className="container">
      <Row>
        <LoginCol md={4} mdOffset={4} xs={6} xsOffset={3}>
          {children}
        </LoginCol>
      </Row>
    </div>
  );
};

LoginBox.propTypes = {
  children: PropTypes.node.isRequired,
};

export default LoginBox;
