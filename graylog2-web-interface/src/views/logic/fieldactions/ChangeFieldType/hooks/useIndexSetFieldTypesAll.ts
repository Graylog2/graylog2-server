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
import { useQuery } from '@tanstack/react-query';

import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import type { IndexSetFieldTypeJson } from 'components/indices/IndexSetFieldTypes/hooks/useIndexSetFieldType';

export type FieldOptions = Array<{ value: string, label: string, disabled: boolean }>;
export type CurrentTypes = Record<string, string>;
const INITIAL_DATA = {
  options: [],
  currentTypes: {},
};

const fetchIndexSetFieldTypesAll = async (indexSetId: string) => {
  const indexSetFieldTypeAllUrl = qualifyUrl(`/system/indices/index_sets/types/${indexSetId}/all`);

  return fetch('GET', indexSetFieldTypeAllUrl).then(
    (elements) => ({
      currentTypes: Object.fromEntries(elements.map((fieldType: IndexSetFieldTypeJson) => ([fieldType.field_name, fieldType.type]))),
      options: elements.map((fieldType: IndexSetFieldTypeJson) => ({
        value: fieldType.field_name,
        label: fieldType.field_name,
        disabled: fieldType.is_reserved === true,
      })),
    }));
};

const useIndexSetFieldTypesAll = (indexSetId: string): {
  data: { options: FieldOptions, currentTypes: CurrentTypes },
  isLoading: boolean,
  refetch: () => void,
} => {
  const { data, isLoading, refetch } = useQuery(
    ['indexSetFieldTypesAll', indexSetId],
    () => fetchIndexSetFieldTypesAll(indexSetId),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading index field types failed with status: ${errorThrown}`,
          'Could not load index field types');
      },
      keepPreviousData: true,
      enabled: !!indexSetId,
    },
  );

  return ({
    data: data ?? INITIAL_DATA,
    isLoading,
    refetch,
  });
};

export default useIndexSetFieldTypesAll;
