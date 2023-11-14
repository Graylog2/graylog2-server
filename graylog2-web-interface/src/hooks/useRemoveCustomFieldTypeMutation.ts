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
import UserNotification from 'preflight/util/UserNotification';

export const urlPrefix = '/system/indices/mappings/remove_mapping';

export type RemoveFieldTypeBody = {
    indexSets: Array<string>,
    fields: Array<string>,
    rotated: boolean,
}

export type RemoveFieldTypeBodyJson = {
    index_sets: Array<string>,
    fields: Array<string>,
    rotate: boolean,
}

const putFieldType = async ({
  indexSets,
  fields,
  rotated,
}: RemoveFieldTypeBody) => {
  const url = qualifyUrl(urlPrefix);
  const body: RemoveFieldTypeBodyJson = {
    index_sets: indexSets,
    fields,
    rotate: rotated,
  };

  return fetch('PUT', url, body);
};

const useRemoveCustomFieldTypeMutation = () => {
  const queryClient = useQueryClient();

  const put = useMutation(putFieldType, {
    onError: (errorThrown) => {
      UserNotification.error(`Removing custom field type failed with status: ${errorThrown}`,
        'Could Removing custom the field type');
    },
    onSuccess: () => {
      UserNotification.success('Custom field type removed successfully', 'Success!');
      queryClient.refetchQueries({ queryKey: ['indexSetFieldTypes'], type: 'active' });
    },
  });

  return { removeCustomFieldTypeMutation: put.mutateAsync, isLoading: put.isLoading };
};

export default useRemoveCustomFieldTypeMutation;
