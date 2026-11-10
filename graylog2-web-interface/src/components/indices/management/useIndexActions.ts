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

import { ClusterDeflector, IndexerIndicesManagementActions } from '@graylog/server-api';

import StringUtils from 'util/StringUtils';
import UserNotification from 'util/UserNotification';

import { INDICES_QUERY_KEY } from './fetchIndices';
import type { IndexAction, IndexActionKey, IndexSummary } from './types';
import { INDEX_OVERVIEW_QUERY_KEY } from './useIndexOverview';

const MAX_LISTED_FAILURES = 10;

type Outcome = { succeeded: number; failures: Array<{ name: string; explanation: string }> };

const fromBulkResponse = (response: {
  successfully_performed: number;
  failures: Array<{ entity_id: string; failure_explanation: string }>;
}): Outcome => ({
  succeeded: response.successfully_performed,
  failures: (response.failures ?? []).map(({ entity_id, failure_explanation }) => ({
    name: entity_id,
    explanation: failure_explanation,
  })),
});

const ACTIONS: { [key in Exclude<IndexActionKey, 'rotate'>]: typeof IndexerIndicesManagementActions.close } = {
  close: IndexerIndicesManagementActions.close,
  open: IndexerIndicesManagementActions.open,
  delete: IndexerIndicesManagementActions.remove,
  flush: IndexerIndicesManagementActions.flush,
  clear_cache: IndexerIndicesManagementActions.clearCache,
  force_merge: IndexerIndicesManagementActions.forceMerge,
};

// Rotating is per index set, through Graylog's own bulk cycle (which runs on the leader node).
const rotate = async (indices: Array<IndexSummary>): Promise<Outcome> => {
  const indexSetIds = [...new Set(indices.map((index) => index.index_set_id))];
  const { success, entity, error_text } = await ClusterDeflector.bulkcycle({ entity_ids: indexSetIds });

  if (!success) {
    throw new Error(error_text || 'Rotating the index sets failed.');
  }

  return fromBulkResponse(entity);
};

const run = async (action: IndexAction, indices: Array<IndexSummary>): Promise<Outcome> =>
  action.key === 'rotate'
    ? rotate(indices)
    : fromBulkResponse(await ACTIONS[action.key]({ entity_ids: indices.map((index) => index.index) }));

const notify = (action: IndexAction, { succeeded, failures }: Outcome) => {
  const what = action.key === 'rotate' ? 'index set' : 'index';
  const subject = StringUtils.pluralize(succeeded, what, what === 'index' ? 'indices' : 'index sets');

  if (failures.length === 0) {
    UserNotification.success(`${action.label}: ${succeeded} ${subject} ${action.pastTense}.`);

    return;
  }

  const listed = failures
    .slice(0, MAX_LISTED_FAILURES)
    .map(({ name, explanation }) => `${name}: ${explanation}`)
    .join('\n');
  const more = failures.length > MAX_LISTED_FAILURES ? `\n…and ${failures.length - MAX_LISTED_FAILURES} more` : '';

  UserNotification.warning(
    `${succeeded} succeeded, ${failures.length} failed:\n${listed}${more}`,
    `${action.label}: some failed`,
  );
};

type RunActionRequest = { action: IndexAction; indices: Array<IndexSummary> };

const useIndexActions = (): {
  runAction: (request: RunActionRequest) => Promise<Outcome>;
  isRunning: boolean;
} => {
  const queryClient = useQueryClient();

  const { mutateAsync, isPending } = useMutation({
    mutationFn: ({ action, indices }: RunActionRequest) => run(action, indices),
    onSuccess: (outcome, { action }) => notify(action, outcome),
    onError: (error, { action }) => UserNotification.error(`${action.label} failed: ${error.message}`),
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: INDICES_QUERY_KEY });
      queryClient.invalidateQueries({ queryKey: INDEX_OVERVIEW_QUERY_KEY });
    },
  });

  return { runAction: mutateAsync, isRunning: isPending };
};

export default useIndexActions;
