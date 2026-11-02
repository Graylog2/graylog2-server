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

import { StreamRules } from '@graylog/server-api';

import type { StreamRule } from 'logic/streams/types';
import UserNotification from 'util/UserNotification';
import i18n from 'i18n';

type StreamRuleData = Pick<StreamRule, 'field' | 'type' | 'value' | 'inverted' | 'description'>;
type StreamRuleMutationData = StreamRuleData & Partial<StreamRule>;

const toStreamRuleData = ({ field, type, value, inverted, description }: StreamRuleMutationData): StreamRuleData => ({
  field,
  type,
  value,
  inverted,
  description,
});

const useStreamRuleMutations = () => {
  const queryClient = useQueryClient();

  const invalidateStreamQueries = () => {
    queryClient.invalidateQueries({ queryKey: ['stream'] });
    queryClient.invalidateQueries({ queryKey: ['streams'] });
  };

  const createRule = useMutation({
    mutationFn: ({ streamId, data }: { streamId: string; data: StreamRuleMutationData }) =>
      StreamRules.create(streamId, toStreamRuleData(data)),
    onSuccess: () => invalidateStreamQueries(),
    onError: (error: Error) => {
      UserNotification.error(i18n.t('use_stream_rule_mutations_notifications.create_failed', { ns: 'streams', error }), i18n.t('use_stream_rule_mutations_notifications.create_failed_title', { ns: 'streams' }));
    },
  });

  const updateRule = useMutation({
    mutationFn: ({
      streamId,
      streamRuleId,
      data,
    }: {
      streamId: string;
      streamRuleId: string;
      data: StreamRuleMutationData;
    }) => StreamRules.update(streamId, streamRuleId, toStreamRuleData(data)),
    onSuccess: () => invalidateStreamQueries(),
    onError: (error: Error) => {
      UserNotification.error(i18n.t('use_stream_rule_mutations_notifications.update_failed', { ns: 'streams', error }), i18n.t('use_stream_rule_mutations_notifications.update_failed_title', { ns: 'streams' }));
    },
  });

  const removeRule = useMutation({
    mutationFn: ({ streamId, streamRuleId }: { streamId: string; streamRuleId: string }) =>
      StreamRules.remove(streamId, streamRuleId),
    onSuccess: () => invalidateStreamQueries(),
    onError: (error: Error) => {
      UserNotification.error(i18n.t('use_stream_rule_mutations_notifications.delete_failed', { ns: 'streams', error }), i18n.t('use_stream_rule_mutations_notifications.delete_failed_title', { ns: 'streams' }));
    },
  });

  return {
    createStreamRule: createRule.mutateAsync,
    updateStreamRule: updateRule.mutateAsync,
    removeStreamRule: removeRule.mutateAsync,
  };
};

export default useStreamRuleMutations;
