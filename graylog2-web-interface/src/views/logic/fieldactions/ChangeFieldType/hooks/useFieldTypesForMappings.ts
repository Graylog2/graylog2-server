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

import { SystemFieldTypes } from '@graylog/server-api';

import type { FieldTypes } from 'views/logic/fieldactions/ChangeFieldType/types';
import { defaultOnError } from 'util/conditional/onError';

const INITIAL_DATA = {
  fieldTypes: {},
};

const fetchFieldTypes = async () => {
  const fieldTypes = await SystemFieldTypes.getAllFieldTypes();

  return ({ fieldTypes });
};

const useFieldTypesForMappings = (): {
  data: { fieldTypes: FieldTypes },
  isLoading: boolean,
} => {
  const { data, isLoading } = useQuery(
    ['fieldTypeOptions'],
    () => defaultOnError(fetchFieldTypes(), 'Loading field type options failed with status', 'Could not load field type options'),
    {
      keepPreviousData: true,
    },
  );

  return ({
    data: data ?? INITIAL_DATA,
    isLoading,
  });
};

export default useFieldTypesForMappings;
