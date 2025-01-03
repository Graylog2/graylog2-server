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
import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query';

import UserNotification from 'util/UserNotification';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import type { EntityGroupsListResponse } from 'components/entity-groups/Types';
import type FetchError from 'logic/errors/FetchError';

const fetchEntityGroups = async () => fetch('GET', qualifyUrl('/entity_groups'));

const createEntityGroup = async (requestObj) => {
  const requestBody = requestObj;

  return fetch('POST', qualifyUrl('/entity_groups'), requestBody);
};

const updateEntityGroup = async ({ id, requestObj }: {id: string, requestObj: { entities?: any, name: string }}) => {
  const requestBody = requestObj;

  return fetch('PUT', qualifyUrl(`/entity_groups/${id}`), requestBody);
};

const deleteEntityGroup = async ({ entityGroupId }: { entityGroupId: string }) => fetch('DELETE', qualifyUrl(`/entity_groups/${entityGroupId}`));

export const useGetEntityGroups = () => {
  const { data, isInitialLoading } = useQuery<EntityGroupsListResponse[], FetchError>(
    ['get-entity-groups'],
    fetchEntityGroups,
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading entity groups failed with status: ${errorThrown}`,
          'Could not load entity groups.');
      },
    },
  );

  return ({
    data: data,
    isInitialLoading,
  });
};

export const useCreateEntityGroup = () => {
  const queryClient = useQueryClient();

  const { mutateAsync, isLoading } = useMutation(
    createEntityGroup,
    {
      onSuccess: () => {
        UserNotification.success('New entity group added successfully');
        queryClient.invalidateQueries(['get-entity-groups']);
      },
      onError: (error: Error) => UserNotification.error(error.message),
    },
  );

  return {
    createEntityGroup: mutateAsync,
    creatingEntityGroup: isLoading,
  };
};

export const useUpdateEntityGroup = () => {
  const queryClient = useQueryClient();

  const { mutateAsync, isLoading } = useMutation(
    updateEntityGroup,
    {
      onSuccess: () => {
        UserNotification.success('Entity group updated successfully');
        queryClient.invalidateQueries(['get-entity-groups']);
      },
      onError: (error: Error) => UserNotification.error(error.message),
    },
  );

  return {
    updateEntityGroup: mutateAsync,
    updatingEntityGroup: isLoading,
  };
};

export const useDeleteEntityGroup = () => {
  const queryClient = useQueryClient();

  const { mutateAsync, isLoading } = useMutation(
    deleteEntityGroup,
    {
      onSuccess: () => {
        UserNotification.success('Entity group deleted successfully');
        queryClient.invalidateQueries(['get-entity-groups']);
      },
      onError: (error: Error) => UserNotification.error(error.message),
    },
  );

  return {
    deleteEntityGroup: mutateAsync,
    deletingEntityGroup: isLoading,
  };
};
