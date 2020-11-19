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
// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Col, Row } from 'components/graylog';
import Footer from 'components/layout/Footer';
import AppContentGrid from 'components/layout/AppContentGrid';

type Props = {
  children: React.Node,
};

const StyledRow = styled(Row)`
  margin-bottom: 0;
`;

const AppWithoutSearchBar = ({ children }: Props) => (
  <AppContentGrid>
    <StyledRow>
      <Col md={12}>
        {children}
      </Col>
    </StyledRow>
    <Footer />
  </AppContentGrid>
);

AppWithoutSearchBar.propTypes = {
  children: PropTypes.node.isRequired,
};

export default AppWithoutSearchBar;
