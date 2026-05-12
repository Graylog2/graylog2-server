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
import { useMutation, useQueryClient } from "@tanstack/react-query";

import { SystemNotifications } from "@graylog/server-api";

import UserNotification from "util/UserNotification";
import useCurrentUser from "hooks/useCurrentUser";
import type FetchError from "logic/errors/FetchError";
import type { NotificationType } from "components/notifications/types";
import {
  NOTIFICATIONS_QUERY_KEY,
  BADGE_COUNT_KEY,
  TABLE_KEY,
} from "components/notifications/constants";

import { type PageShape, type Snapshot, isPageShape } from "./pageShape";

const patchPages =
  (id: string, patch: Partial<NotificationType>) =>
  (data: PageShape | undefined): PageShape | undefined => {
    if (!isPageShape(data)) return data;

    return {
      ...data,
      elements: data.elements.map((row) =>
        row.id === id ? { ...row, ...patch } : row,
      ),
    };
  };

const useNotificationToggleRead = () => {
  const queryClient = useQueryClient();
  const currentUser = useCurrentUser();
  const tableKey = [...NOTIFICATIONS_QUERY_KEY, TABLE_KEY] as const;
  const badgeKey = [...NOTIFICATIONS_QUERY_KEY, BADGE_COUNT_KEY] as const;

  return useMutation<
    NotificationType,
    FetchError,
    { id: string; currentIsRead: boolean },
    { snapshot: Snapshot }
  >({
    mutationFn: ({ id }) =>
      SystemNotifications.toggleRead(id) as Promise<NotificationType>,

    onMutate: async ({ id, currentIsRead }) => {
      await queryClient.cancelQueries({ queryKey: tableKey });

      const snapshot = queryClient.getQueriesData<PageShape>({
        queryKey: tableKey,
      });

      const optimistic: Partial<NotificationType> = {
        is_read: !currentIsRead,
        last_changed: new Date().toISOString(),
        actor: { id: currentUser.id, name: currentUser.username },
      };

      queryClient.setQueriesData(
        { queryKey: tableKey },
        patchPages(id, optimistic),
      );

      return { snapshot };
    },

    onError: (error, _vars, context) => {
      context?.snapshot.forEach(([key, data]) => {
        queryClient.setQueryData(key, data);
      });

      if (error?.status === 403) {
        UserNotification.error(
          "You do not have permission to change this notification.",
          "Action not allowed",
        );

        return;
      }

      if (error?.status === 404) {
        UserNotification.warning(
          "Notification no longer exists.",
          "Notification not found",
        );
        queryClient.invalidateQueries({ queryKey: tableKey });

        return;
      }

      UserNotification.error(
        "Failed to update notification read state. Please try again.",
        "Update failed",
      );
    },

    onSuccess: (serverEntity) => {
      // patch with server response — backend may override actor/timestamps set optimistically
      queryClient.setQueriesData(
        { queryKey: tableKey },
        patchPages(serverEntity.id, serverEntity),
      );
    },

    onSettled: (_data, error) => {
      // Skip invalidation on 403 — the server state is unchanged.
      if (error?.status === 403) return;

      queryClient.invalidateQueries({ queryKey: tableKey });
      queryClient.invalidateQueries({ queryKey: badgeKey });
    },
  });
};

export default useNotificationToggleRead;
