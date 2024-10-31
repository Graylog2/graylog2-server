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

import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import type {
  SetIndexSetFieldTypeProfileBody,
  SetIndexSetFieldTypeProfileBodyJson,
} from 'components/indices/IndexSetFieldTypes/types';

export const urlPrefix = '/system/indices/mappings/set_profile';

const putProfile = async ({
  indexSetId,
  profileId,
  rotated,
}: SetIndexSetFieldTypeProfileBody) => {
  const url = qualifyUrl(urlPrefix);
  const body: SetIndexSetFieldTypeProfileBodyJson = {
    index_sets: [indexSetId],
    rotate: rotated,
    profile_id: profileId,
  };

  return fetch('PUT', url, body);
};

const useSetIndexSetProfileMutation = () => {
  const queryClient = useQueryClient();

  const put = useMutation(putProfile, {
    onError: (errorThrown) => {
      UserNotification.error(`Setting index set profile failed with status: ${errorThrown}`,
        'Could not set index set profile');
    },
    onSuccess: () => {
      UserNotification.success('Set index set profile successfully', 'Success!');

      return queryClient.refetchQueries({ queryKey: ['indexSetFieldTypes'], type: 'active' });
    },
  });

  return { setIndexSetFieldTypeProfile: put.mutateAsync, isLoading: put.isLoading };
};

export default useSetIndexSetProfileMutation;
