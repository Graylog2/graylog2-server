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
  const { streams } = eventDefinition.config;
  const timeRange: RelativeTimeRangeStartOnly = {
    type: 'relative',
    range: eventDefinition.config.search_within_ms / 1000,
  };
  const queryString: ElasticsearchQueryString = {
    type: 'elasticsearch',
    query_string: eventDefinition?.config?.query || '',
  };
  const groupBy = eventDefinition.config.group_by;

  return useMemo(
    () => ViewGenerator({ streams, timeRange, queryString, aggregations, groupBy }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [],
  );
};

export default useCreateViewForEventDefinition;
