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
  RemoveProfileFromIndexSetBodyJson,
  RemoveProfileFromIndexSetBody,
} from 'components/indices/IndexSetFieldTypes/types';

export const urlPrefix = '/system/indices/mappings/remove_profile_from';

const putRemoveProfileFromIndex = async ({
  indexSetId,
  rotated,
}: RemoveProfileFromIndexSetBody) => {
  const url = qualifyUrl(urlPrefix);
  const body: RemoveProfileFromIndexSetBodyJson = {
    index_sets: [indexSetId],
    rotate: rotated,
  };

  return fetch('PUT', url, body);
};

const useRemoveProfileFromIndexMutation = () => {
  const queryClient = useQueryClient();

  const put = useMutation(putRemoveProfileFromIndex, {
    onError: (errorThrown) => {
      UserNotification.error(`Removing profile from index failed with status: ${errorThrown}`,
        'Could not remove profile from index');
    },
    onSuccess: () => {
      UserNotification.success('Removed profile from index successfully', 'Success!');

      return queryClient.refetchQueries({ queryKey: ['indexSetFieldTypes'], type: 'active' });
    },
  });

  return { removeProfileFromIndex: put.mutateAsync, isLoading: put.isLoading };
};

export default useRemoveProfileFromIndexMutation;
