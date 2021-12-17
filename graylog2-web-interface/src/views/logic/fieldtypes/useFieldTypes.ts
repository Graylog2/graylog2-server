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
import { useQuery } from 'react-query';

import type { TimeRange } from 'views/logic/queries/Query';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import type { FieldTypeMappingJSON } from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';

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

const useFieldTypes = (streams: Array<string> = [], timerange: TimeRange): { data: FieldTypeMapping[] } => useQuery([streams, timerange], () => fetchAllFieldTypes(streams, timerange));

export default useFieldTypes;
