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
import mapValues from 'lodash/mapValues';

import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import type { RemoveFieldTypeBody, RemoveFieldTypeBodyJson, IndexSetFieldType, IndexSetFieldTypeJson } from 'components/indices/IndexSetFieldTypes/types';

export const urlPrefix = '/system/indices/mappings/remove_mapping';

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

type IndexSetResponseJSON = {
  successfully_performed: number,
  failures: Array<{ entity_id: string, failure_explanation: string }>,
  errors: Array<string>,
  succeeded: Array<IndexSetFieldTypeJson>,
}
type RemovalResponseJSON = Record<string, IndexSetResponseJSON>

export type IndexSetResponse = {
  indexSetId: string,
  successfullyPerformed: number,
  failures: Array<{ entityId: string, failureExplanation: string }>,
  errors: Array<string>,
  succeeded: Array<IndexSetFieldType>,
}
export type RemovalResponse = Record<string, IndexSetResponse>

const useRemoveCustomFieldTypeMutation = (params: { onErrorHandler: (response: RemovalResponse) => void, onSuccessHandler: (response: RemovalResponse) => void }) => {
  const queryClient = useQueryClient();

  const put = useMutation(putFieldType, {
    onError: (errorThrown) => {
      UserNotification.error(`Removing custom field type failed with status: ${errorThrown}`,
        'Could not remove custom field type');
    },
    onSuccess: (response: RemovalResponseJSON) => {
      let errorsQuantity: number = 0;
      const mappedResponse: RemovalResponse = mapValues(response, ({ successfully_performed, succeeded, errors, failures: failuresJSON }, id) => {
        const failures = failuresJSON.map(({ entity_id, failure_explanation }) => ({ entityId: entity_id, failureExplanation: failure_explanation }));
        errorsQuantity = errorsQuantity + failures.length + errors.length;

        return ({
          indexSetId: id,
          successfullyPerformed: successfully_performed,
          succeeded: succeeded.map(({ field_name, type, origin, is_reserved }) => ({
            isReserved: is_reserved,
            fieldName: field_name,
            type,
            origin,
            id: field_name,
          })),
          errors,
          failures,
        });
      },
      );

      queryClient.refetchQueries({ queryKey: ['indexSetFieldTypes'], type: 'active' });

      if (errorsQuantity === 0) {
        UserNotification.success('Custom field type removed successfully', 'Success!');

        return params.onSuccessHandler(mappedResponse);
      }

      return params.onErrorHandler(mappedResponse);
    },
  });

  return { removeCustomFieldTypeMutation: put.mutateAsync, isLoading: put.isLoading };
};

export default useRemoveCustomFieldTypeMutation;
