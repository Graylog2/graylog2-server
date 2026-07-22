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
import { useCallback } from 'react';

import AutoRefreshProvider from 'views/components/contexts/AutoRefreshProvider';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import useViewsSelector from 'views/stores/useViewsSelector';
import { selectJobIds } from 'views/logic/slices/searchExecutionSelectors';
import { executeActiveQuery } from 'views/logic/slices/viewSlice';
import { useSearchURLQueryParams } from 'views/logic/NormalizeSearchURLQueryParams';
import { durationToMS } from 'util/DateTime';
import useMinimumRefreshInterval from 'views/hooks/useMinimumRefreshInterval';

const SearchPageAutoRefreshProvider = ({ children }: React.PropsWithChildren) => {
  const dispatch = useViewsDispatch();
  const jobIds = useViewsSelector(selectJobIds);
  const { autoRefresh } = useSearchURLQueryParams();

  const { data: minimumRefresh, isInitialLoading: isLoadingMinimumRefresh } = useMinimumRefreshInterval();
  const minimumRefreshInterval = minimumRefresh ? durationToMS(minimumRefresh) : 0;
  const autoRefreshInterval = durationToMS(autoRefresh);
  const interval = minimumRefreshInterval > autoRefreshInterval ? minimumRefreshInterval : autoRefreshInterval;

  const refreshConfig = autoRefresh && !isLoadingMinimumRefresh ? { interval: interval, enabled: true } : null;

  const onRefresh = useCallback(() => {
    if (!jobIds) {
      dispatch(executeActiveQuery());
    }
  }, [dispatch, jobIds]);

  return (
    <AutoRefreshProvider
      key={isLoadingMinimumRefresh ? 'loading' : 'ready'}
      defaultRefreshConfig={refreshConfig}
      onRefresh={onRefresh}>
      {children}
    </AutoRefreshProvider>
  );
};

export default SearchPageAutoRefreshProvider;
