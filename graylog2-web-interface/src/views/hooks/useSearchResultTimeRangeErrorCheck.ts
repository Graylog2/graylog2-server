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
import { useCallback, useMemo } from 'react';
import isEqual from 'lodash/isEqual';

import useSearchResult from 'views/hooks/useSearchResult';
import useCurrentQuery from 'views/logic/queries/useCurrentQuery';
import type { TimeRange } from 'views/logic/queries/Query';
import useIsLoading from 'views/hooks/useIsLoading';

const useSearchResultTimeRangeErrorCheck = (errorType: string) => {
  const searchResult = useSearchResult();
  const currentQuery = useCurrentQuery();
  const isLoadingExecution = useIsLoading();

  const searchResultErrors = useMemo(
    () => searchResult?.result?.errors?.filter((error) => error.queryId === currentQuery?.id) ?? [],
    [currentQuery?.id, searchResult?.result?.errors],
  );

  const timeRangeHasErrorInResults = useMemo(
    () => searchResultErrors.some(({ type }) => type === errorType),
    [errorType, searchResultErrors],
  );

  return useCallback(
    (currentTimeRange: TimeRange | {}) => {
      const executedTimerange = currentQuery?.timerange;

      return !isLoadingExecution && isEqual(currentTimeRange, executedTimerange) && timeRangeHasErrorInResults;
    },
    [currentQuery?.timerange, isLoadingExecution, timeRangeHasErrorInResults],
  );
};

export default useSearchResultTimeRangeErrorCheck;
