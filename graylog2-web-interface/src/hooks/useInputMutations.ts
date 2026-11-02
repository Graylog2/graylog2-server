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

import { SystemInputs } from '@graylog/server-api';

import UserNotification from 'util/UserNotification';

import { KEY_PREFIX } from './usePaginatedInputs';
import i18n from 'i18n';

const createInput = async ({ input }): Promise<{ id: string }> => SystemInputs.create(input);
const updateInput = async ({ input, inputId }): Promise<{ id: string }> => SystemInputs.update(input, inputId);
const deleteInput = async ({ inputId }): Promise<void> => SystemInputs.terminate(inputId);

const useInputMutations = () => {
  const queryClient = useQueryClient();

  const createMutation = useMutation({
    mutationFn: createInput,
    onError: (errorThrown) => {
      UserNotification.error(
        i18n.t('use_input_mutations_notifications.save_failed', { ns: 'inputs', error: errorThrown }),
        i18n.t('use_input_mutations_notifications.save_failed_title', { ns: 'inputs' }),
      );
    },
    onSuccess: () => {
      UserNotification.success(
        i18n.t('use_input_mutations_notifications.created_success', { ns: 'inputs' }),
        i18n.t('use_input_mutations_notifications.success_title', { ns: 'inputs' }),
      );
      queryClient.invalidateQueries({ queryKey: KEY_PREFIX });
    },
  });
  const updateMutation = useMutation({
    mutationFn: updateInput,
    onError: (errorThrown) => {
      UserNotification.error(
        i18n.t('use_input_mutations_notifications.update_failed', { ns: 'inputs', error: errorThrown }),
        i18n.t('use_input_mutations_notifications.update_failed_title', { ns: 'inputs' }),
      );
    },
    onSuccess: () => {
      UserNotification.success(
        i18n.t('use_input_mutations_notifications.updated_success', { ns: 'inputs' }),
        i18n.t('use_input_mutations_notifications.success_title', { ns: 'inputs' }),
      );
      queryClient.invalidateQueries({ queryKey: ['inputs'] });
    },
  });
  const deleteMutation = useMutation({
    mutationFn: deleteInput,
    onError: (errorThrown) => {
      UserNotification.error(
        i18n.t('use_input_mutations_notifications.delete_failed', { ns: 'inputs', error: errorThrown }),
        i18n.t('use_input_mutations_notifications.delete_failed_title', { ns: 'inputs' }),
      );
    },
    onSuccess: () => {
      UserNotification.success(
        i18n.t('use_input_mutations_notifications.deleted_success', { ns: 'inputs' }),
        i18n.t('use_input_mutations_notifications.success_title', { ns: 'inputs' }),
      );
      queryClient.invalidateQueries({ queryKey: KEY_PREFIX });
    },
  });

  return {
    createInput: createMutation.mutateAsync,
    updateInput: updateMutation.mutateAsync,
    deleteInput: deleteMutation.mutateAsync,
  };
};

export default useInputMutations;
