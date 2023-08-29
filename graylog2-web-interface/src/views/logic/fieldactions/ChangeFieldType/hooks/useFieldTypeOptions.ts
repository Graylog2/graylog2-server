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
import startCase from 'lodash/startCase';

import UserNotification from 'util/UserNotification';
import type { SearchParams, Attribute } from 'stores/PaginationTypes';
import { qualifyUrl } from 'util/URLUtils';
import PaginationURL from 'util/PaginationURL';
import type { FieldTypeUsage, FieldTypeOptions } from 'views/logic/fieldactions/ChangeFieldType/types';

const INITIAL_DATA = {
  options: [],
};

const fieldTypeUsagesUrl = qualifyUrl('/system/indices/mappings/type_names');

const fetchFieldTypeOptions = async () => {
  const url = fieldTypeUsagesUrl;

  // fetch('GET', qualifyUrl(fieldTypeUsagesUrl))
  const types = await Promise.resolve(['type_1', 'type_2', 'type_3', 'type_4', 'type_5']);

  return ({
    options: types.map((type) => ({
      id: type,
      label: startCase(type),
    })),
  });
};

const useFiledTypeOptions = (): {
  data: { options: FieldTypeOptions },
  isLoading: boolean,
} => {
  const { data, isLoading } = useQuery(
    ['fieldTypeOptions'],
    () => fetchFieldTypeOptions(),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading field type options failed with status: ${errorThrown}`,
          'Could not load field type options');
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
