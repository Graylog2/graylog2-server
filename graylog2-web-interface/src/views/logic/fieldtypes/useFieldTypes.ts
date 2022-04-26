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
import { useMemo } from 'react';
import { useQuery } from 'react-query';

import type { TimeRange } from 'views/logic/queries/Query';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import type { FieldTypeMappingJSON } from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import { adjustFormat, toUTCFromTz } from 'util/DateTime';
import useUserDateTime from 'hooks/useUserDateTime';

const fieldTypesUrl = qualifyUrl('/views/fields');

type FieldTypesResponse = Array<FieldTypeMappingJSON>;

type FieldTypesRequest = {
  streams?: Array<string>,
  timerange?: TimeRange,
};

const _deserializeFieldTypes = (response: FieldTypesResponse) => response
  .map((fieldTypeMapping) => FieldTypeMapping.fromJSON(fieldTypeMapping));

const createFieldTypeRequest = (streams: Array<string>, timerange: TimeRange): FieldTypesRequest => {
  let request: FieldTypesRequest = {};

  if (streams && streams.length > 0) {
    request = { streams };
  }

  if (timerange) {
    request = { ...request, timerange };
  }

  return request;
};

const fetchAllFieldTypes = (streams: Array<string>, timerange: TimeRange): Promise<Array<FieldTypeMapping>> => fetch('POST', fieldTypesUrl, createFieldTypeRequest(streams, timerange))
  .then(_deserializeFieldTypes);

const normalizeTimeRange = (timerange: TimeRange, userTz: string): TimeRange => {
  switch (timerange?.type) {
    case 'absolute':
      return {
        type: 'absolute',
        from: adjustFormat(toUTCFromTz(timerange.from, userTz), 'internal'),
        to: adjustFormat(toUTCFromTz(timerange.to, userTz), 'internal'),
      };
    default:
      return timerange;
  }
};

const useFieldTypes = (streams: Array<string>, timerange: TimeRange): { data: FieldTypeMapping[] } => {
  const { userTimezone } = useUserDateTime();
  const _timerange = useMemo(() => normalizeTimeRange(timerange, userTimezone), [timerange, userTimezone]);

  return useQuery(
    ['fieldTypes', streams, _timerange],
    () => fetchAllFieldTypes(streams, _timerange),
    { staleTime: 30000, refetchOnWindowFocus: false, cacheTime: 0 },
  );
};

export default useFieldTypes;
