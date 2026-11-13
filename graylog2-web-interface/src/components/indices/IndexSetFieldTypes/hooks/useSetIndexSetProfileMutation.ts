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
  SetIndexSetFieldTypeProfileBody,
  SetIndexSetFieldTypeProfileBodyJson,
} from 'components/indices/IndexSetFieldTypes/types';
import {
  parseProfileChangeResponse,
  refetchAfterProfileChange,
  resultsWithProblems,
} from 'components/indices/IndexSetFieldTypes/profileChangeResult';

const putProfile = async ({ indexSetIds, profileId, rotated }: SetIndexSetFieldTypeProfileBody) => {
  const body: SetIndexSetFieldTypeProfileBodyJson = {
    index_sets: indexSetIds,
    rotate: rotated,
    profile_id: profileId,
  };

  return SystemFieldTypes.bulkSetProfile(body).then(parseProfileChangeResponse);
};

const useSetIndexSetProfileMutation = (): {
  setIndexSetFieldTypeProfile: (body: SetIndexSetFieldTypeProfileBody) => Promise<ProfileChangeResponse>;
  isLoading: boolean;
} => {
  const queryClient = useQueryClient();

  const put = useMutation({
    mutationFn: putProfile,

    onError: (errorThrown) => {
      UserNotification.error(
        `Setting index set profile failed with status: ${errorThrown}`,
        'Could not set index set profile',
      );
    },

    onSuccess: (response) => {
      if (resultsWithProblems(response).length === 0) {
        UserNotification.success('Set index set profile successfully', 'Success!');
      }

      return refetchAfterProfileChange(queryClient);
    },
  });

  return { setIndexSetFieldTypeProfile: put.mutateAsync, isLoading: put.isPending };
};

export default useSetIndexSetProfileMutation;
