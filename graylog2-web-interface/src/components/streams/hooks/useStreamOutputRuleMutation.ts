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

import { StreamDestinationsFilters } from '@graylog/server-api';

import i18n from 'i18n';
import UserNotification from 'util/UserNotification';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import type { StreamOutputFilterRule } from 'components/streams/StreamDetails/output-filter/Types';

type StreamOutputParam = { streamId: string; filterOutputRule: Partial<StreamOutputFilterRule> };

const createStreamOutputRule = async ({ streamId, filterOutputRule }: StreamOutputParam) =>
  fetch('POST', qualifyUrl(ApiRoutes.StreamOutputFilterRuleApiController.create(streamId).url), filterOutputRule);
const updateStreamOutputRule = async ({ streamId, filterOutputRule }: StreamOutputParam) =>
  fetch(
    'PUT',
    qualifyUrl(ApiRoutes.StreamOutputFilterRuleApiController.update(streamId, filterOutputRule.id).url),
    filterOutputRule,
  );
const removeStreamOutputRule = async ({ streamId, filterId }: { streamId: string; filterId: string }) =>
  StreamDestinationsFilters.deleteFilter(streamId, filterId);

const useStreamOutputRuleMutation = () => {
  const queryClient = useQueryClient();
  const invalidateStreamQueries = () =>
    queryClient.invalidateQueries({
      queryKey: ['streams'],
    });

  const createMutation = useMutation({
    mutationFn: createStreamOutputRule,

    onError: (errorThrown) => {
      UserNotification.error(
        i18n.t('stream_hooks.create_rule_failed', { ns: 'streams', error: errorThrown }),
        i18n.t('stream_hooks.create_rule_failed_title', { ns: 'streams' }),
      );
    },

    onSuccess: () => {
      UserNotification.success(
        i18n.t('stream_hooks.create_rule_success', { ns: 'streams' }),
        i18n.t('stream_hooks.success_title', { ns: 'streams' }),
      );
      invalidateStreamQueries();
    },
  });
  const updateMutation = useMutation({
    mutationFn: updateStreamOutputRule,

    onError: (errorThrown) => {
      UserNotification.error(
        i18n.t('stream_hooks.update_rule_failed', { ns: 'streams', error: errorThrown }),
        i18n.t('stream_hooks.update_rule_failed_title', { ns: 'streams' }),
      );
    },

    onSuccess: () => {
      UserNotification.success(
        i18n.t('stream_hooks.update_rule_success', { ns: 'streams' }),
        i18n.t('stream_hooks.success_title', { ns: 'streams' }),
      );
      invalidateStreamQueries();
    },
  });
  const removeMutation = useMutation({
    mutationFn: removeStreamOutputRule,

    onError: (errorThrown) => {
      UserNotification.error(
        i18n.t('stream_hooks.delete_rule_failed', { ns: 'streams', error: errorThrown }),
        i18n.t('stream_hooks.delete_rule_failed_title', { ns: 'streams' }),
      );
    },

    onSuccess: () => {
      UserNotification.success(
        i18n.t('stream_hooks.delete_rule_success', { ns: 'streams' }),
        i18n.t('stream_hooks.success_title', { ns: 'streams' }),
      );
      invalidateStreamQueries();
    },
  });

  return {
    createStreamOutputRule: createMutation.mutateAsync,
    updateStreamOutputRule: updateMutation.mutateAsync,
    removeStreamOutputRule: removeMutation.mutateAsync,
  };
};

export default useStreamOutputRuleMutation;
