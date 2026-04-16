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
import { useMemo } from 'react';
import { useQueries } from '@tanstack/react-query';

import { SystemInputs } from '@graylog/server-api';

import useCurrentUser from 'hooks/useCurrentUser';
import { isPermitted } from 'util/PermissionsMixin';
import { onError } from 'util/conditional/onError';
import FetchError from 'logic/errors/FetchError';
import UserNotification from 'util/UserNotification';

import { useCollectorInputIds } from './useCollectorInputIds';

export const useCollectorInputDetails = () => {
  const currentUser = useCurrentUser();
  const { data: collectorInputIds = [], isLoading: isLoadingIds } = useCollectorInputIds();

  const readableInputIds = useMemo(
    () => collectorInputIds.filter((id) => isPermitted(currentUser?.permissions, `inputs:read:${id}`)),
    [collectorInputIds, currentUser?.permissions],
  );

  const inputQueries = useQueries({
    queries: readableInputIds.map((id) => ({
      queryKey: ['inputs', id],
      queryFn: () =>
        onError(SystemInputs.get(id), (error) => {
          if (error instanceof FetchError && error.status === 404) return;

          UserNotification.error(
            `Loading collector input details failed with status: ${error}`,
            'Could not load collector input details.',
          );
        }),
      retry: false,
      refetchOnWindowFocus: true, // override global false — refresh input data when user returns to this tab
    })),
  });

  const allQueriesSettled = inputQueries.every((q) => !q.isLoading);

  const loadedInputs = inputQueries.filter((q) => q.isSuccess && q.data).map((q) => q.data);

  const unreadableCount = collectorInputIds.length - readableInputIds.length;

  return {
    collectorInputIds,
    readableInputIds,
    loadedInputs,
    unreadableCount,
    isLoading: isLoadingIds || !allQueriesSettled,
  };
};
