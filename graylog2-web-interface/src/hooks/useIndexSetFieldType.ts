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
import type { FieldTypes } from 'views/logic/fieldactions/ChangeFieldType/types';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';

const INITIAL_DATA = {
  fieldTypes: {},
};

export const url = (indexSetId) => qualifyUrl(`/system/indices/index_sets/types/${indexSetId}`);
const fetchIndexSetFieldTypes = async (indexSetId: string) => fetch('GET', url(indexSetId));

const useFiledTypeOptions = (indexSetId: string): {
    data: { fieldTypes: FieldTypes },
    isLoading: boolean,
} => {
  const { data, isLoading } = useQuery(
    ['indexSetFieldTypes'],
    () => fetchIndexSetFieldTypes(indexSetId),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading index field types failed with status: ${errorThrown}`,
          'Could not load index field types');
      },
      keepPreviousData: true,
    },
  );

  return ({
    data: data ?? INITIAL_DATA,
    isLoading,
  });
};

export default useFiledTypeOptions;
