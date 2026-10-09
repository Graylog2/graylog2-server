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

import { SystemFieldTypes } from '@graylog/server-api';

import UserNotification from 'util/UserNotification';
import type {
  ProfileChangeResponse,
  RemoveProfileFromIndexSetBodyJson,
  RemoveProfileFromIndexSetBody,
} from 'components/indices/IndexSetFieldTypes/types';
import {
  parseProfileChangeResponse,
  refetchAfterProfileChange,
  resultsWithProblems,
} from 'components/indices/IndexSetFieldTypes/profileChangeResult';

const putRemoveProfileFromIndex = async ({ indexSetIds, rotated }: RemoveProfileFromIndexSetBody) => {
  const body: RemoveProfileFromIndexSetBodyJson = {
    index_sets: indexSetIds,
    rotate: rotated,
  };

  return SystemFieldTypes.bulkRemoveProfile(body).then(parseProfileChangeResponse);
};

const useRemoveProfileFromIndexMutation = (): {
  removeProfileFromIndex: (body: RemoveProfileFromIndexSetBody) => Promise<ProfileChangeResponse>;
  isLoading: boolean;
} => {
  const queryClient = useQueryClient();

  const put = useMutation({
    mutationFn: putRemoveProfileFromIndex,

    onError: (errorThrown) => {
      UserNotification.error(
        `Removing profile from index failed with status: ${errorThrown}`,
        'Could not remove profile from index',
      );
    },

    onSuccess: (response) => {
      if (resultsWithProblems(response).length === 0) {
        UserNotification.success('Removed profile from index successfully', 'Success!');
      }

      return refetchAfterProfileChange(queryClient);
    },
  });

  return { removeProfileFromIndex: put.mutateAsync, isLoading: put.isPending };
};

export default useRemoveProfileFromIndexMutation;
