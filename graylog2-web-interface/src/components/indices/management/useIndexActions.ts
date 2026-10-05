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
import { useMutation, useQueryClient } from '@tanstack/react-query';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';

import type { IndexAction, IndexActionResponse, IndexActionResult } from './types';
import { INDEX_OVERVIEW_URL, INDEX_OVERVIEW_QUERY_KEY } from './useIndexOverview';

const MAX_LISTED_FAILURES = 10;

type RunActionRequest = { action: IndexAction; indices: Array<string> };

const notifyResults = (action: IndexAction, results: Array<IndexActionResult>) => {
  const failed = results.filter((result) => !result.ok);
  const succeeded = results.length - failed.length;

  if (failed.length === 0) {
    UserNotification.success(`${action.label}: ${succeeded} ${succeeded === 1 ? 'index' : 'indices'} ${action.pastTense}.`);

    return;
  }

  const listed = failed
    .slice(0, MAX_LISTED_FAILURES)
    .map((result) => `${result.index}: ${result.message}`)
    .join('\n');
  const more = failed.length > MAX_LISTED_FAILURES ? `\n…and ${failed.length - MAX_LISTED_FAILURES} more` : '';

  UserNotification.warning(
    `${succeeded} succeeded, ${failed.length} failed:\n${listed}${more}`,
    `${action.label}: some indices failed`,
  );
};

const useIndexActions = (): {
  runAction: (request: RunActionRequest) => Promise<IndexActionResponse>;
  isRunning: boolean;
} => {
  const queryClient = useQueryClient();

  const { mutateAsync, isPending } = useMutation({
    mutationFn: ({ action, indices }: RunActionRequest) =>
      fetch<IndexActionResponse>('POST', qualifyUrl(`${INDEX_OVERVIEW_URL}/actions/${action.key}`), { indices }),
    onSuccess: (response, { action }) => notifyResults(action, response.results),
    onError: (error, { action }) => UserNotification.error(`${action.label} failed: ${error.message}`),
    onSettled: () => queryClient.invalidateQueries({ queryKey: INDEX_OVERVIEW_QUERY_KEY }),
  });

  return { runAction: mutateAsync, isRunning: isPending };
};

export default useIndexActions;
