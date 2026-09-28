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
import LoadingPlaceholderContext from 'views/components/contexts/LoadingPlaceholderContext';
import WidgetContentSkeleton from 'views/components/widgets/WidgetContentSkeleton';
import type View from 'views/logic/views/View';

import replayLinkWidgetAction from './ReplayLinkWidgetAction';
import WelcomeSearchSkeleton from './WelcomeSearchSkeleton';
import type { WelcomeSearchWidgetEntry } from './hooks/useWelcomeSearch';

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
  entries: Array<WelcomeSearchWidgetEntry>;
};

/**
 * Renders a search-backed view as a non-interactive, sidebar-less widget area, e.g. for the welcome
 * page's metrics tiles and charts. Shared by any welcome-page area that builds its own view/search with
 * `useWelcomeSearch` (see components/welcome/hooks/useWelcomeSearch.ts) and just needs it rendered.
 * While loading, the widgets are shown as content skeletons.
 */
const WelcomeSearch = ({ view, entries }: Props) => (
  <InteractiveContext.Provider value="read-only">
    <WidgetActionsContext.Provider value={WIDGET_ACTIONS}>
      <LoadingPlaceholderContext.Provider
        value={{ search: <WelcomeSearchSkeleton entries={entries} />, widget: WidgetContentSkeleton }}>
        <SearchPageLayoutProvider value={SEARCH_PAGE_LAYOUT_CONTEXT_VALUE}>
          <SearchPage view={view} isNew={false} skipNoStreamsCheck />
        </SearchPageLayoutProvider>
      </LoadingPlaceholderContext.Provider>
    </WidgetActionsContext.Provider>
  </InteractiveContext.Provider>
);

export default WelcomeSearch;
