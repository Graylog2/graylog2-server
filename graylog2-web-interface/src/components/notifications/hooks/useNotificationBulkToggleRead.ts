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
import UserNotification from 'util/UserNotification';
import useCurrentUser from 'hooks/useCurrentUser';
import type FetchError from 'logic/errors/FetchError';
import type { NotificationType } from 'components/notifications/types';
import {
  NOTIFICATIONS_QUERY_KEY,
  BADGE_COUNT_KEY,
  TABLE_KEY,
} from 'components/notifications/constants';

// Per-row context captured before the optimistic patch so we can flip each row
// from its initial state — a mixed selection (read + unread) produces a mixed
// result rather than forcing every row to the same target.
type RowSeed = { id: string; currentIsRead: boolean };

type PageShape = {
  elements: NotificationType[];
} & Record<string, unknown>;

type Snapshot = Array<[readonly unknown[], PageShape | undefined]>;

const isPageShape = (value: unknown): value is PageShape =>
  typeof value === 'object'
  && value !== null
  && Array.isArray((value as PageShape).elements);

const flipRows = (
  patches: Map<string, Partial<NotificationType>>,
) => (data: PageShape | undefined): PageShape | undefined => {
  if (!isPageShape(data)) return data;

  return {
    ...data,
    elements: data.elements.map((row) => {
      const patch = patches.get(row.id);

      return patch ? { ...row, ...patch } : row;
    }),
  };
};

const useNotificationBulkToggleRead = () => {
  const queryClient = useQueryClient();
  const currentUser = useCurrentUser();
  const tableKey = [...NOTIFICATIONS_QUERY_KEY, TABLE_KEY] as const;
  const badgeKey = [...NOTIFICATIONS_QUERY_KEY, BADGE_COUNT_KEY] as const;

  return useMutation<unknown, FetchError, { rows: RowSeed[] }, { snapshot: Snapshot }>({
    mutationFn: ({ rows }) =>
      fetch('POST', '/system/notifications/bulk/toggle_read', { entity_ids: rows.map(({ id }) => id) }),

    onMutate: async ({ rows }) => {
      await queryClient.cancelQueries({ queryKey: tableKey });

      const snapshot = queryClient.getQueriesData<PageShape>({ queryKey: tableKey });
      const now = new Date().toISOString();

      const patches = new Map<string, Partial<NotificationType>>(
        rows.map(({ id, currentIsRead }) => [
          id,
          {
            is_read: !currentIsRead,
            last_changed: now,
            actor: { id: currentUser.id, name: currentUser.username },
          },
        ]),
      );

      queryClient.setQueriesData({ queryKey: tableKey }, flipRows(patches));

      return { snapshot };
    },

    onError: (error, _vars, context) => {
      context?.snapshot.forEach(([key, data]) => {
        queryClient.setQueryData(key, data);
      });

      if (error?.status === 403) {
        UserNotification.error(
          'You do not have permission to update one or more selected notifications.',
          'Action not allowed',
        );

        return;
      }

      if (error?.status === 400) {
        UserNotification.warning('No notifications selected.', 'Nothing to update');

        return;
      }

      UserNotification.error(
        'Failed to update notification read states. Please try again.',
        'Update failed',
      );
    },

    // Backend silently drops unknown ids in the request and returns 204; partial
    // success is therefore indistinguishable from full success on the wire. We
    // re-fetch so the displayed table reflects the authoritative server state
    // (rows that didn't actually flip drop back to their original is_read).
    // 403 is skipped — server state is unchanged, no point thrashing the cache.
    onSettled: (_data, error) => {
      if (error?.status === 403) return;

      queryClient.invalidateQueries({ queryKey: tableKey });
      queryClient.invalidateQueries({ queryKey: badgeKey });
    },
  });
};

export default useNotificationBulkToggleRead;
