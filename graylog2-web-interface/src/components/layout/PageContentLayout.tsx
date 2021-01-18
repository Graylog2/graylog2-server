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

import WithGlobalAppNotifications from 'components/notifications/WithGlobalAppNotifications';
import { Grid } from 'components/graylog';
import Footer from 'components/layout/Footer';

type Props = {
  children: React.ReactNode,
  className?: string
};

const Container = styled.div`
  display: flex;
  flex-direction: column;
  overflow: auto;
  height: 100%;
  width: 100%;

  /* Bottom gap is defined by footer bottom margin */
  padding: 15px 15px 0 15px;
`;

const StyledGrid = styled(Grid)`
  width: 100%;
  flex: 1;
  margin-bottom: 10px;
`;

/*
 * Provides the basic layout for the page content section.
 * The section includes all page specific components, but not elements like the navigation or sidebar.
 */
const PageContentLayout = ({ children, className }: Props) => (
  <Container className={className}>
    <WithGlobalAppNotifications>
      <StyledGrid fluid className="page-content-grid">
        {children}
      </StyledGrid>
      <Footer />
    </WithGlobalAppNotifications>
  </Container>
);

PageContentLayout.propTypes = {
  children: PropTypes.node.isRequired,
  className: PropTypes.string,
};

PageContentLayout.defaultProps = {
  className: undefined,
};

export default PageContentLayout;
