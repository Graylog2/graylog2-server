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

import { useContext, useMemo } from 'react';

import type { MetricUnitType } from 'views/types';
import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import useActiveQueryId from 'views/hooks/useActiveQueryId';

type FieldUnits = Record<string, { unit_type: MetricUnitType, unit: string }>

const useQueryFieldTypes = () => {
  const fieldTypes = useContext(FieldTypesContext);
  const queryId = useActiveQueryId();

  return useMemo(() => fieldTypes.queryFields.get(queryId, fieldTypes.all), [fieldTypes.all, fieldTypes.queryFields, queryId]);
};

const useDefinedFieldUnits = () => useMemo<FieldUnits>(() => {
  const fieldTypes = useContext(FieldTypesContext);
  // const fieldTypes2 = useQueryFieldTypes();
  // const { data, isLoading } = useFieldTypes(undefined, undefined);
  console.log('!!!!!!!!', { ft: fieldTypes?.all, ff: fieldTypes?.queryFields });

  return ({
    http_response_code: {
      unit_type: 'size',
      unit:
      'bytes',
    }
    ,
  });
}, []);

export default useDefinedFieldUnits;
