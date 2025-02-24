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
import * as React from 'react';
import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import styled from 'styled-components';

import type { SearchParams } from 'stores/PaginationTypes';
import Spinner from 'components/common/Spinner';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot, { DateType } from 'views/logic/aggregationbuilder/Pivot';
import LineVisualization from 'views/components/visualizations/line/LineVisualization';
import Series from 'views/logic/aggregationbuilder/Series';
import type { Row } from 'views/logic/searchtypes/pivot/PivotHandler';
import { fetchEventsHistogram } from 'components/events/fetchEvents';

/*
{
  "key": [
    "2025-02-21T15:07:20.000Z"
  ],
  "values": [
    {
      "key": [
        "count()"
      ],
      "value": 17058,
      "rollup": true,
      "source": "row-leaf"
    },
    {
      "key": [
        "example.org",
        "count()"
      ],
      "value": 17058,
      "rollup": false,
      "source": "col-leaf"
    }
  ],
  "source": "leaf"
}
*/

const createLeaf = (date: string, row: string, count: number): Row => ({
  'key': [date],
  'values': [
    {
      'key': [row, 'count()'],
      'value': count,
      'rollup': false,
      'source': 'col-leaf',
    },
  ],
  'source': 'leaf',
});

const config = AggregationWidgetConfig.builder()
  .visualization('line')
  .rowPivots([Pivot.create(['timestamp'], DateType)])
  .columnPivots([Pivot.createValues(['type'])])
  .series([Series.forFunction('count()')])
  .rollup(false)
  .build();

const GraphContainer = styled.div`
  height: 180px;
  width: 100%;
  margin: 20px 0;
`;

const EventsGraph = ({
  data,
  effectiveTimerange,
}: Pick<React.ComponentProps<typeof LineVisualization>, 'data' | 'effectiveTimerange'>) => {
  const store = useMemo(
    () =>
      configureStore({
        reducer: (state) => state,
        preloadedState: { view: { activeQuery: 'deadbeef' } },
      }),
    [],
  );

  return (
    <Provider store={store}>
      <GraphContainer>
        <LineVisualization height={80} config={config} data={data} effectiveTimerange={effectiveTimerange} />
      </GraphContainer>
    </Provider>
  );
};

type Props = {
  searchParams: SearchParams;
};

const EventsHistogram = ({ searchParams }: Props) => {
  const { data, isInitialLoading } = useQuery(['events', 'histogram', searchParams], () =>
    fetchEventsHistogram(searchParams),
  );

  const chartData = data?.results
    ? {
        chart: [
          ...data.results.buckets.events.map((bucket) => createLeaf(bucket.start_date, 'Events', bucket.count)),
          ...data.results.buckets.alerts.map((bucket) => createLeaf(bucket.start_date, 'Alerts', bucket.count)),
        ],
      }
    : {};

  return isInitialLoading ? <Spinner /> : <EventsGraph data={chartData} />;
};

export default EventsHistogram;
