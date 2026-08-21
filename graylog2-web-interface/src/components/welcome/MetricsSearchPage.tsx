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
import { forwardRef, useMemo } from 'react';

import useSearchConfiguration from 'hooks/useSearchConfiguration';
import { durationInSeconds } from 'util/DateTime';
import InteractiveContext from 'views/components/contexts/InteractiveContext';
import { BLANK } from 'views/components/contexts/SearchPageLayoutContext';
import SearchPageLayoutProvider from 'views/components/contexts/SearchPageLayoutProvider';
import SearchPage from 'views/pages/SearchPage';
import WidgetActionsContext from 'views/components/contexts/WidgetActionsContext';

import useWelcomeMetricsSearch, { DEFAULT_TIME_RANGE_SECONDS } from './hooks/useWelcomeMetricsSearch';
import replayLinkWidgetAction from './ReplayLinkWidgetAction';

const WIDGET_ACTIONS = [replayLinkWidgetAction];

const SearchAreaContainer = forwardRef<HTMLDivElement, React.PropsWithChildren>(({ children }, ref) => (
  <div ref={ref}>{children}</div>
));

type Props = {
  topSourcesOnly?: boolean;
};

const MetricsSearchPage = ({ topSourcesOnly = false }: Props) => {
  const { config } = useSearchConfiguration();
  const queryTimeRangeLimitSeconds = durationInSeconds(config?.query_time_range_limit ?? '');
  const rangeSeconds =
    queryTimeRangeLimitSeconds > 0 && queryTimeRangeLimitSeconds < DEFAULT_TIME_RANGE_SECONDS
      ? queryTimeRangeLimitSeconds
      : DEFAULT_TIME_RANGE_SECONDS;

  const view = useWelcomeMetricsSearch(rangeSeconds, topSourcesOnly);

  const searchPageLayoutContextValue = useMemo(
    () => ({
      sidebar: { isShown: false },
      viewActions: BLANK,
      searchAreaContainer: { component: SearchAreaContainer },
    }),
    [],
  );

  return (
    <InteractiveContext.Provider value={false}>
      <WidgetActionsContext.Provider value={WIDGET_ACTIONS}>
        <SearchPageLayoutProvider value={searchPageLayoutContextValue}>
          <SearchPage view={view} isNew={false} skipNoStreamsCheck />
        </SearchPageLayoutProvider>
      </WidgetActionsContext.Provider>
    </InteractiveContext.Provider>
  );
};

export default MetricsSearchPage;
