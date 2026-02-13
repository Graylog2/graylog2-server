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
import { useMemo } from 'react';
import styled from 'styled-components';

import SearchPageLayoutProvider from 'views/components/contexts/SearchPageLayoutProvider';
import type { LayoutState } from 'views/components/contexts/SearchPageLayoutContext';
import { SAVE_COPY } from 'views/components/contexts/SearchPageLayoutContext';
import { Row } from 'components/bootstrap';

const Container = styled(Row)`
  position: relative;
`;

const PageContentLayout = styled.div`
  padding: 0 15px;
  width: 100%;
  height: 100%;
`;

const SearchAreaContainer = React.forwardRef<HTMLDivElement, React.PropsWithChildren>(({ children }, ref) => (
  <PageContentLayout ref={ref}>{children}</PageContentLayout>
));

type Props = React.PropsWithChildren<{
  viewActions?: LayoutState['viewActions'];
}>;

const NoSidebarSearchLayout = ({ children, viewActions = SAVE_COPY }: Props) => {
  const searchPageLayout = useMemo(
    () => ({
      sidebar: { isShown: false },
      viewActions,
      searchAreaContainer: { component: SearchAreaContainer },
    }),
    [viewActions],
  );

  return (
    <SearchPageLayoutProvider value={searchPageLayout}>
      <Container>{children}</Container>
    </SearchPageLayoutProvider>
  );
};

export default NoSidebarSearchLayout;
