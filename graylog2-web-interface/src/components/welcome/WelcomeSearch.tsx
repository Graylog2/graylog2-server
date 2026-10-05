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
import { forwardRef } from 'react';

import InteractiveContext from 'views/components/contexts/InteractiveContext';
import { BLANK } from 'views/components/contexts/SearchPageLayoutContext';
import SearchPageLayoutProvider from 'views/components/contexts/SearchPageLayoutProvider';
import SearchPage from 'views/pages/SearchPage';
import WidgetActionsContext from 'views/components/contexts/WidgetActionsContext';
import type View from 'views/logic/views/View';

import replayLinkWidgetAction from './ReplayLinkWidgetAction';

const WIDGET_ACTIONS = [replayLinkWidgetAction];

const SearchAreaContainer = forwardRef<HTMLDivElement, React.PropsWithChildren>(({ children }, ref) => (
  <div ref={ref}>{children}</div>
));

const SEARCH_PAGE_LAYOUT_CONTEXT_VALUE = {
  sidebar: { isShown: false },
  viewActions: BLANK,
  searchAreaContainer: { component: SearchAreaContainer },
};

type Props = {
  view: Promise<View>;
};

/**
 * Renders a search-backed view as a non-interactive, sidebar-less widget area, e.g. for the welcome
 * page's metrics tiles and charts. Shared by any welcome-page area that builds its own view/search with
 * `useWelcomeSearch` (see components/welcome/hooks/useWelcomeSearch.ts) and just needs it rendered.
 */
const WelcomeSearch = ({ view }: Props) => (
  <InteractiveContext.Provider value="read-only">
    <WidgetActionsContext.Provider value={WIDGET_ACTIONS}>
      <SearchPageLayoutProvider value={SEARCH_PAGE_LAYOUT_CONTEXT_VALUE}>
        <SearchPage view={view} isNew={false} skipNoStreamsCheck />
      </SearchPageLayoutProvider>
    </WidgetActionsContext.Provider>
  </InteractiveContext.Provider>
);

export default WelcomeSearch;
