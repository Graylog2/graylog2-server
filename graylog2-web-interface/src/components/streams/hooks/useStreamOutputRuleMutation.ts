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
import { useMutation } from '@tanstack/react-query';

import { StreamDestinationsFilters } from '@graylog/server-api';
import UserNotification from 'util/UserNotification';

import type { StreamOutputFilterRule } from '../StreamDetails/common/Types';

type StreamOutputParam = {streamId: string, filterOutputRule: Partial<StreamOutputFilterRule>};
const createStreamOutputRule = async ({ streamId, filterOutputRule }: StreamOutputParam) => StreamDestinationsFilters.createFilter(streamId, filterOutputRule);
const updateStreamOutputRule = async ({ streamId, filterOutputRule }: StreamOutputParam) => StreamDestinationsFilters.updateFilter(streamId, filterOutputRule.id, filterOutputRule);
const removeStreamOutputRule = async ({ streamId, filterId }: {streamId: string, filterId: string}) => StreamDestinationsFilters.deleteFilter(streamId, filterId);

const useStreamOutputRuleMutation = () => {
  const createMutation = useMutation(createStreamOutputRule, {
    onError: (errorThrown) => {
      UserNotification.error(`Create stream output filter rule failed with status: ${errorThrown}`,
        'Could not create stream output filter rule');
    },
    onSuccess: () => {
      UserNotification.success('Stream Output filter rule has been successfully created.', 'Success!');
    },
  });
  const updateMutation = useMutation(updateStreamOutputRule, {
    onError: (errorThrown) => {
      UserNotification.error(`Updating strean output filter rule failed with status: ${errorThrown}`,
        'Could not update stream output filter rule');
    },
    onSuccess: () => {
      UserNotification.success('Stream Output filter rule has been successfully updated.', 'Success!');
    },

  });
  const removeMutation = useMutation(removeStreamOutputRule, {
    onError: (errorThrown) => {
      UserNotification.error(`Deleting stream output filter rule failed with status: ${errorThrown}`,
        'Could not delete stream output filter rule');
    },
    onSuccess: () => {
      UserNotification.success(' Stream Output filter rule has been successfully removed.', 'Success!');
    },
  });

  return {
    createStreamOutputRule: createMutation.mutateAsync,
    updateStreamOutputRule: updateMutation.mutateAsync,
    removeStreamOutputRule: removeMutation.mutateAsync,
  };
};

export default useStreamOutputRuleMutation;
