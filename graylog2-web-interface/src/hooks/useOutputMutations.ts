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

import { SystemOutputs } from '@graylog/server-api';

import UserNotification from 'util/UserNotification';
import { KEY_PREFIX } from 'hooks/useOutputs';
import i18n from 'i18n';

const useOutputMutations = () => {
  const queryClient = useQueryClient();

  const invalidateOutputQueries = () => {
    queryClient.invalidateQueries({ queryKey: KEY_PREFIX });
  };

  const saveMutation = useMutation({
    mutationFn: (output: { title: string; type: string; configuration: Record<string, unknown> }) =>
      SystemOutputs.create(output as any),
    onSuccess: () => invalidateOutputQueries(),
    onError: (error: Error, output) => {
      UserNotification.error(i18n.t('use_output_mutations_notifications.save_failed', { ns: 'streams', title: output.title, error }), i18n.t('use_output_mutations_notifications.save_failed_title', { ns: 'streams' }));
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ outputId, deltas }: { outputId: string; title: string; deltas: any }) =>
      SystemOutputs.update(outputId, deltas),
    onSuccess: () => invalidateOutputQueries(),
    onError: (error: Error, { title }) => {
      UserNotification.error(i18n.t('use_output_mutations_notifications.update_failed', { ns: 'streams', title, error }), i18n.t('use_output_mutations_notifications.update_failed_title', { ns: 'streams' }));
    },
  });

  const removeMutation = useMutation({
    mutationFn: (outputId: string) => SystemOutputs.remove(outputId),
    onSuccess: () => invalidateOutputQueries(),
    onError: (error: Error) => {
      UserNotification.error(i18n.t('use_output_mutations_notifications.terminate_failed', { ns: 'streams', error }), i18n.t('use_output_mutations_notifications.terminate_failed_title', { ns: 'streams' }));
    },
  });

  return {
    saveOutput: saveMutation.mutateAsync,
    updateOutput: updateMutation.mutateAsync,
    removeOutput: removeMutation.mutateAsync,
  };
};

export default useOutputMutations;
