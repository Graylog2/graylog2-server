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
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';

export type EventDefinitionTag = {
  id: string;
  value: string;
};

const TAGS_URL = '/events/tags';
const TAGS_QUERY_KEY = ['event-definitions', 'tags'] as const;

const fetchTags = (): Promise<Array<EventDefinitionTag>> =>
  fetch('GET', qualifyUrl(`${TAGS_URL}/all`));

export const useEventDefinitionTags = () => {
  const { data, isLoading, isError } = useQuery({
    queryKey: TAGS_QUERY_KEY,
    queryFn: fetchTags,
  });

  return {
    tags: data ?? [],
    loadingTags: isLoading,
    tagsLoadError: isError,
  };
};

export const useEventDefinitionTagMutations = () => {
  const queryClient = useQueryClient();
  const invalidate = () => queryClient.invalidateQueries({ queryKey: TAGS_QUERY_KEY });

  const addMutation = useMutation({
    mutationFn: (value: string) => fetch('POST', qualifyUrl(TAGS_URL), value),
    onSuccess: () => {
      invalidate();
      UserNotification.success('Tag added successfully.');
    },
    onError: (error: Error) => {
      UserNotification.error(`Adding tag failed: ${error.message}`, 'Could not add tag');
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, value }: { id: string; value: string }) =>
      fetch('PUT', qualifyUrl(`${TAGS_URL}/${id}`), value),
    onSuccess: () => {
      invalidate();
      UserNotification.success('Tag updated successfully.');
    },
    onError: (error: Error) => {
      UserNotification.error(`Updating tag failed: ${error.message}`, 'Could not update tag');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => fetch('DELETE', qualifyUrl(`${TAGS_URL}/${id}`)),
    onSuccess: () => {
      invalidate();
      UserNotification.success('Tag deleted successfully.');
    },
    onError: (error: Error) => {
      UserNotification.error(`Deleting tag failed: ${error.message}`, 'Could not delete tag');
    },
  });

  return {
    addTag: addMutation.mutateAsync,
    addingTag: addMutation.isPending,
    updateTag: ({ id, value }: { id: string; value: string }) => updateMutation.mutateAsync({ id, value }),
    updatingTag: updateMutation.isPending,
    deleteTag: deleteMutation.mutateAsync,
    deletingTag: deleteMutation.isPending,
  };
};
