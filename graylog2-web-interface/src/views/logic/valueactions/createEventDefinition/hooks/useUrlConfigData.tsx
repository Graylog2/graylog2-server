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

import type { Checked, MappedData } from 'views/logic/valueactions/createEventDefinition/types';
import type { EventDefinitionURLConfig } from 'components/event-definitions/hooks/useEventDefinitionConfigFromUrl';
import { concatQueryStrings } from 'views/logic/queries/QueryHelper';

const concatQuery = (queryParts: Array<string>) => {
  return concatQueryStrings(queryParts.filter((queryPart) => !!queryPart));
};

const useUrlConfigData = ({ mappedData, checked }: { mappedData: MappedData, checked: Checked }) => useMemo<EventDefinitionURLConfig>(() => {
  const {
    queryWithReplacedParams,
    searchFilterQuery,
    searchFromValue,
    columnValuePath,
    rowValuePath,
    lutParameters,
    searchWithinMs,
    streams,
    aggValue,
    aggFunction,
    aggField,
    columnGroupBy,
    rowGroupBy,
  } = mappedData;

  const queries: Array<string> = Object.entries({
    queryWithReplacedParams, searchFilterQuery, searchFromValue, columnValuePath, rowValuePath,
  }).filter(([key]) => checked[key]).map(([_, search]) => search);

  const getAggregations = (): Partial<EventDefinitionURLConfig> => {
    const res: Partial<EventDefinitionURLConfig> = {};

    if (checked.aggCondition) {
      res.agg_field = aggField;
      res.agg_value = aggValue;
      res.agg_function = aggFunction;
    }

    if (checked.columnGroupBy) {
      res.group_by = columnGroupBy;
    }

    if (checked.rowGroupBy) {
      res.group_by = (rowGroupBy || []).concat(res.group_by || []);
    }

    return res;
  };

  const getRest = () => {
    const res: Partial<EventDefinitionURLConfig> = {};
    if (checked.lutParameters && lutParameters && lutParameters.length) res.loc_query_parameters = lutParameters;
    if (checked.searchWithinMs && searchWithinMs) res.search_within_ms = searchWithinMs;
    if (checked.streams && streams && streams.length) res.streams = streams;

    return res;
  };

  return ({
    type: 'aggregation-v1',
    query: concatQuery(queries),
    ...getAggregations(),
    ...getRest(),
  });
}, [checked, mappedData]);

export default useUrlConfigData;
