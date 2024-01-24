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

import type { EventDefinition } from 'logic/alerts/types';
import type { EventDefinitionAggregation } from 'hooks/useEventDefinition';
import type { ElasticsearchQueryString, RelativeTimeRangeStartOnly } from 'views/logic/queries/Query';
import { ViewGenerator } from 'views/logic/views/UseCreateViewForEvent';

const useCreateViewForEventDefinition = (
  {
    eventDefinition,
    aggregations,
  }: { eventDefinition: EventDefinition, aggregations: Array<EventDefinitionAggregation> },
) => {
  const streams = eventDefinition?.config?.streams ?? [];
  const timeRange: RelativeTimeRangeStartOnly = {
    type: 'relative',
    range: (eventDefinition?.config?.search_within_ms ?? 0) / 1000,
  };
  const queryString: ElasticsearchQueryString = {
    type: 'elasticsearch',
    query_string: eventDefinition?.config?.query || '',
  };

  const queryParameters = eventDefinition?.config?.query_parameters || [];

  const groupBy = eventDefinition?.config?.group_by ?? [];

  const searchFilters = eventDefinition?.config?.filters ?? [];

  return useMemo(
    () => ViewGenerator({ streams, timeRange, queryString, aggregations, groupBy, queryParameters, searchFilters }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [],
  );
};

export default useCreateViewForEventDefinition;
