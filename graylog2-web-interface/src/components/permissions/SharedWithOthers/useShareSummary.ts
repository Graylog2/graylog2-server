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
import { useQuery } from '@tanstack/react-query';
import { create, indexedResolver, windowScheduler } from '@yornaath/batshit';

import { AuthorizationShares } from '@graylog/server-api';

import { defaultOnError } from 'util/conditional/onError';

import type { ShareSummary } from './shareCounts';

// Batches the requests of the cells rendered together, e.g. the cells of a table page.
// Handles errors per batch, so a failed request shows one notification instead of one per cell.
const shareSummaries = create({
  fetcher: (grns: Array<string>) =>
    defaultOnError(
      AuthorizationShares.getSummaries({ grns }),
      'Loading sharing information failed with status',
      'Could not load sharing information',
    ).then(({ summaries }) => summaries),
  resolver: indexedResolver(),
  scheduler: windowScheduler(10),
});

const useShareSummary = (
  grn: string,
): {
  data: ShareSummary | null | undefined;
  isInitialLoading: boolean;
  isError: boolean;
} => {
  const { data, isInitialLoading, isError } = useQuery({
    queryKey: ['authz', 'shares', 'summary', grn],
    // An entity without counted grantees is not part of the response.
    queryFn: () => shareSummaries.fetch(grn).then((summary) => summary ?? null),
    // The batch fetcher already shows a notification, so a retry would show another one.
    retry: false,
  });

  return { data, isInitialLoading, isError };
};

export default useShareSummary;
