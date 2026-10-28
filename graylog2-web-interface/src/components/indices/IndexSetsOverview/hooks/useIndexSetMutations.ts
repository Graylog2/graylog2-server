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
import i18n from 'i18n';

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
      UserNotification.success(
        i18n.t('overview_table.default_set_success', { ns: 'indexsets', title: indexSet.title }),
        i18n.t('overview_table.success', { ns: 'indexsets' }),
      );

      return invalidateIndexSets();
    },
    onError: (error, indexSet) => {
      UserNotification.error(
        i18n.t('overview_table.default_set_failed', { ns: 'indexsets', title: indexSet.title, error }),
        i18n.t('overview_table.default_set_failed_title', { ns: 'indexsets' }),
      );
    },
  });

  const deleteMutation = useMutation({
    mutationFn: ({ indexSet, deleteIndices }: DeleteIndexSetParams) =>
      SystemIndexSets.remove(indexSet.id, deleteIndices),
    onSuccess: (_response, { indexSet }) => {
      UserNotification.success(
        i18n.t('overview_table.delete_success', { ns: 'indexsets', title: indexSet.title }),
        i18n.t('overview_table.success', { ns: 'indexsets' }),
      );

      return invalidateIndexSets();
    },
    onError: (error, { indexSet }) => {
      UserNotification.error(
        i18n.t('overview_table.delete_failed', { ns: 'indexsets', title: indexSet.title, error }),
        i18n.t('overview_table.delete_failed_title', { ns: 'indexsets' }),
      );
    },
  });

  return {
    setDefaultIndexSet: setDefaultMutation.mutate,
    deleteIndexSet: deleteMutation.mutate,
  };
};

export default useIndexSetMutations;
