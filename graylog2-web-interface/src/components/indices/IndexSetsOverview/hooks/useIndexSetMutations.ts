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

import { SystemIndexSets } from '@graylog/server-api';

import UserNotification from 'util/UserNotification';

import { KEY_PREFIX } from '../fetchIndexSets';
import type { IndexSetEntity } from '../types';

type DeleteIndexSetParams = {
  indexSet: IndexSetEntity;
  deleteIndices: boolean;
};

const useIndexSetMutations = (): {
  setDefaultIndexSet: (indexSet: IndexSetEntity) => void;
  deleteIndexSet: (params: DeleteIndexSetParams) => void;
} => {
  const queryClient = useQueryClient();
  const invalidateIndexSets = () => queryClient.invalidateQueries({ queryKey: KEY_PREFIX });

  const setDefaultMutation = useMutation({
    mutationFn: (indexSet: IndexSetEntity) => SystemIndexSets.setDefault(indexSet.id),
    onSuccess: (_response, indexSet) => {
      UserNotification.success(`Successfully set index set '${indexSet.title}' as default`, 'Success');

      return invalidateIndexSets();
    },
    onError: (error, indexSet) => {
      UserNotification.error(
        `Setting index set '${indexSet.title}' as default failed with status: ${error}`,
        'Could not set default index set.',
      );
    },
  });

  const deleteMutation = useMutation({
    mutationFn: ({ indexSet, deleteIndices }: DeleteIndexSetParams) =>
      SystemIndexSets.remove(indexSet.id, deleteIndices),
    onSuccess: (_response, { indexSet }) => {
      UserNotification.success(`Successfully deleted index set '${indexSet.title}'`, 'Success');

      return invalidateIndexSets();
    },
    onError: (error, { indexSet }) => {
      UserNotification.error(
        `Deleting index set '${indexSet.title}' failed with status: ${error}`,
        'Could not delete index set.',
      );
    },
  });

  return {
    setDefaultIndexSet: setDefaultMutation.mutate,
    deleteIndexSet: deleteMutation.mutate,
  };
};

export default useIndexSetMutations;
