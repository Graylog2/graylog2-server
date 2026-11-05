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

import { ClusterInputState } from '@graylog/server-api';

import UserNotification from 'util/UserNotification';
import type { InputSummary } from 'hooks/usePaginatedInputs';
import { INPUT_STATES_QUERY_KEY } from 'hooks/useInputsStates';
import i18n from 'i18n';

type PROPS = {
  inputId: string;
};

const startInput = async ({ inputId }: PROPS) => ClusterInputState.start(inputId);
const stopInput = async ({ inputId }: PROPS) => ClusterInputState.stop(inputId);
const setupInput = async ({ inputId }: PROPS) => ClusterInputState.setup(inputId);

const useInputStateMutations = (input: InputSummary) => {
  const queryClient = useQueryClient();

  const startMutation = useMutation({
    mutationFn: startInput,
    onError: (error) => {
      UserNotification.error(
        i18n.t('use_input_state_mutations_notifications.start_failed', { ns: 'inputs', title: input.title, error }),
        i18n.t('use_input_state_mutations_notifications.start_failed_title', { ns: 'inputs', title: input.title }),
      );
    },
    onSuccess: () => {
      UserNotification.success(
        i18n.t('use_input_state_mutations_notifications.started_success', { ns: 'inputs' }),
        i18n.t('use_input_state_mutations_notifications.success_title', { ns: 'inputs' }),
      );
      queryClient.invalidateQueries({ queryKey: INPUT_STATES_QUERY_KEY });
    },
  });
  const stopMutation = useMutation({
    mutationFn: stopInput,
    onError: (error) => {
      UserNotification.error(
        i18n.t('use_input_state_mutations_notifications.stop_failed', { ns: 'inputs', title: input.title, error }),
        i18n.t('use_input_state_mutations_notifications.stop_failed_title', { ns: 'inputs', title: input.title }),
      );
    },
    onSuccess: () => {
      UserNotification.success(
        i18n.t('use_input_state_mutations_notifications.stopped_success', { ns: 'inputs' }),
        i18n.t('use_input_state_mutations_notifications.success_title', { ns: 'inputs' }),
      );
      queryClient.invalidateQueries({ queryKey: INPUT_STATES_QUERY_KEY });
    },
  });
  const setupMutation = useMutation({
    mutationFn: setupInput,
    onError: (error) => {
      UserNotification.error(
        i18n.t('use_input_state_mutations_notifications.setup_failed', { ns: 'inputs', title: input.title, error }),
        i18n.t('use_input_state_mutations_notifications.setup_failed_title', { ns: 'inputs', title: input.title }),
      );
    },
    onSuccess: () => {
      UserNotification.success(
        i18n.t('use_input_state_mutations_notifications.deleted_success', { ns: 'inputs' }),
        i18n.t('use_input_state_mutations_notifications.success_title', { ns: 'inputs' }),
      );
      queryClient.invalidateQueries({ queryKey: INPUT_STATES_QUERY_KEY });
    },
  });

  return {
    startInput: startMutation.mutateAsync,
    stopInput: stopMutation.mutateAsync,
    setupInput: setupMutation.mutateAsync,
  };
};

export default useInputStateMutations;
