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
import { useCallback, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import styled from 'styled-components';

import type { SearchParams } from 'stores/PaginationTypes';
import Spinner from 'components/common/Spinner';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot, { DateType } from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';
import { fetchEventsHistogram, parseTypeFilter } from 'components/events/fetchEvents';
import XYPlot from 'views/components/visualizations/XYPlot';
import FullSizeContainer from 'views/components/aggregationbuilder/FullSizeContainer';
import InteractiveContext from 'views/components/contexts/InteractiveContext';

const config = AggregationWidgetConfig.builder()
  .visualization('line')
  .rowPivots([Pivot.create(['timestamp'], DateType)])
  .columnPivots([Pivot.createValues(['type'])])
  .series([Series.forFunction('count()')])
  .rollup(false)
  .build();

const height = 180;

const GraphContainer = styled.div`
  height: ${height}px;
  width: 100%;
  margin: 20px 0;
`;

type ResultPromise = ReturnType<typeof fetchEventsHistogram>;
type PromiseType<T> = T extends Promise<infer R> ? R : never;

const generateChart = (
  type: 'Alerts' | 'Events',
  buckets: PromiseType<ResultPromise>['results']['buckets']['alerts' | 'events'],
) => {
  const x = buckets.map((b) => b.start_date);
  const y = buckets.map((b) => b.count);

  return {
    type: 'scatter',
    name: type,
    x,
    y,
    originalName: type,
    line: {
      shape: 'linear',
      color: type === 'Alerts' ? '#4478b3' : '#fd9e48',
    },
    yaxis: 'y',
    fullPath: `${type}⸱count()`,
  };
};

const EventsGraph = ({
  data: { results },
  alerts,
}: {
  data: PromiseType<ResultPromise>;
  alerts: 'include' | 'exclude' | 'only';
}) => {
  const store = useMemo(
    () =>
      configureStore({
        reducer: (state) => state,
        preloadedState: { view: { activeQuery: 'deadbeef' } },
      }),
    [],
  );
  const onZoom = useCallback(() => true, []);

  const chartData = useMemo(
    () => [
      ...(['include', 'exclude'].includes(alerts) ? [generateChart('Events', results.buckets.events)] : []),
      ...(['include', 'only'].includes(alerts) ? [generateChart('Alerts', results.buckets.alerts)] : []),
    ],
    [alerts, results.buckets.alerts, results.buckets.events],
  );

  return (
    <Provider store={store}>
      <GraphContainer>
        <InteractiveContext.Provider value={false}>
          <FullSizeContainer>
            {(dimensions) => (
              <XYPlot config={config} chartData={chartData} onZoom={onZoom} height={height} width={dimensions.width} />
            )}
          </FullSizeContainer>
        </InteractiveContext.Provider>
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

  const alerts = parseTypeFilter(searchParams?.filters?.get('alert')?.[0]);

  return isInitialLoading ? <Spinner /> : <EventsGraph data={data} alerts={alerts} />;
};

export default EventsHistogram;
