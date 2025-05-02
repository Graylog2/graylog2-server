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
import styled from 'styled-components';
import { Outlet } from 'react-router-dom';

import WithGlobalAppNotifications from 'components/notifications/WithGlobalAppNotifications';
import { Grid } from 'components/bootstrap';
import Footer from 'components/layout/Footer';
import useFooterCustomization from 'brand-customization/useFooterCustomization';

type Props = React.PropsWithChildren<{
  className?: string;
  FooterComponent?: React.ComponentType;
  NotificationsComponent?: React.ComponentType<{ children: React.ReactNode }>;
}>;

const Container = styled.div(
  ({ theme }) => `
  display: flex;
  flex-direction: column;
  overflow: auto;
  height: 100%;
  width: 100%;

  /* Bottom gap is defined by the footer */
  padding: ${theme.spacings.sm} ${theme.spacings.sm} 0 ${theme.spacings.sm};
`,
);

const StyledGrid = styled(Grid)`
  width: 100%;
  flex: 1;
  margin-bottom: 10px;
`;

/*
 * Provides the basic layout for the page content section.
 * The section includes all page specific components, but not elements like the navigation or sidebar.
 */
const PageContentLayout = ({
  children = null,
  className = undefined,
  FooterComponent = Footer,
  NotificationsComponent = WithGlobalAppNotifications,
}: Props) => {
  const { enabled } = useFooterCustomization();

  return (
    <Container className={className}>
      <NotificationsComponent>
        <StyledGrid fluid className="page-content-grid">
          {children || <Outlet />}
        </StyledGrid>
        <>{enabled && <FooterComponent />}</>
      </NotificationsComponent>
    </Container>
  );
};

export default PageContentLayout;
