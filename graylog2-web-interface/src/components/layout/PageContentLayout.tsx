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
import * as React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Col, Row } from 'components/graylog';
import Footer from 'components/layout/Footer';
import PageContentGrid from 'components/layout/PageContentGrid';

type Props = {
  children: React.ReactNode,
};

const StyledRow = styled(Row)`
  margin-bottom: 0;
`;

/*
 * Provides the basic layout for the page content section.
 * The section includes all page specific components, but not elements like the navigation or sidebar.
 */
const PageContentLayout = ({ children }: Props) => (
  <PageContentGrid>
    <StyledRow>
      <Col md={12}>
        {children}
      </Col>
    </StyledRow>
    <Footer />
  </PageContentGrid>
);

PageContentLayout.propTypes = {
  children: PropTypes.node.isRequired,
};

export default PageContentLayout;
