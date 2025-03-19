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
import styled from 'styled-components';

import Spinner from 'components/common/Spinner';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot, { DateType } from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';
import { fetchEventsHistogram, parseTypeFilter } from 'components/events/fetchEvents';
import FullSizeContainer from 'views/components/aggregationbuilder/FullSizeContainer';
import InteractiveContext from 'views/components/contexts/InteractiveContext';
import PlotLegend from 'views/components/visualizations/PlotLegend';
import GenericPlot, { type PlotLayout, type ChartConfig } from 'views/components/visualizations/GenericPlot';
import type ColorMapper from 'views/components/visualizations/ColorMapper';
import type { MiddleSectionProps } from 'components/common/PaginatedEntityTable/PaginatedEntityTable';
import useUserDateTime from 'hooks/useUserDateTime';
import { toUTCFromTz } from 'util/DateTime';
import useOnRefresh from 'components/common/PaginatedEntityTable/useOnRefresh';

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

const yLegendPosition = (containerHeight: number) => {
  if (containerHeight < 150) {
    return -0.6;
  }

  if (containerHeight < 400) {
    return -0.2;
  }

  return -0.14;
};
const defaultSetColor = (chart: ChartConfig, colors: ColorMapper) => ({
  line: { color: colors.get(chart.originalName ?? chart.name) },
});
const layout: Partial<PlotLayout> = {
  yaxis: {
    fixedrange: true,
    rangemode: 'tozero',
    tickformat: ',~r',
    type: 'linear',
  },
  dragmode: 'zoom',
  hovermode: 'x',
  xaxis: {
    domain: [0, 1],
    type: 'date',
  },
  legend: { y: yLegendPosition(height) },
};

const EventsGraph = ({
  data: { results },
  alerts,
  onZoom,
}: {
  data: PromiseType<ResultPromise>;
  alerts: 'include' | 'exclude' | 'only';
  onZoom: (from: string, to: string) => void;
}) => {
  const chartData = useMemo(
    () => [
      ...(['include', 'exclude'].includes(alerts) ? [generateChart('Events', results.buckets.events)] : []),
      ...(['include', 'only'].includes(alerts) ? [generateChart('Alerts', results.buckets.alerts)] : []),
    ],
    [alerts, results.buckets.alerts, results.buckets.events],
  );

  return (
    <GraphContainer>
      <InteractiveContext.Provider value={false}>
        <FullSizeContainer>
          {(dimensions) => (
            <PlotLegend config={config} chartData={chartData} height={height} width={dimensions.width}>
              <InteractiveContext.Provider value>
                <GenericPlot chartData={chartData} layout={layout} onZoom={onZoom} setChartColor={defaultSetColor} />
              </InteractiveContext.Provider>
            </PlotLegend>
          )}
        </FullSizeContainer>
      </InteractiveContext.Provider>
    </GraphContainer>
  );
};

type EventsHistogramFetcher = typeof fetchEventsHistogram;

type Props = MiddleSectionProps & {
  eventsHistogramFetcher?: EventsHistogramFetcher;
};
const EventsHistogram = ({ searchParams, setFilters, eventsHistogramFetcher = fetchEventsHistogram }: Props) => {
  const { userTimezone, formatTime } = useUserDateTime();
  const { data, isInitialLoading, refetch } = useQuery(
    ['events', 'histogram', searchParams],
    () => eventsHistogramFetcher(searchParams),
    { keepPreviousData: true },
  );

  useOnRefresh(refetch);

  const alerts = parseTypeFilter(searchParams?.filters?.get('alert')?.[0]);
  const onZoom = useCallback(
    (from: string, to: string) => {
      const parsedFrom = formatTime(toUTCFromTz(from, userTimezone), 'internal');
      const parsedTo = formatTime(toUTCFromTz(to, userTimezone), 'internal');
      setFilters(searchParams.filters.set('timestamp', [`${parsedFrom}><${parsedTo}`]));
    },
    [formatTime, searchParams.filters, setFilters, userTimezone],
  );

  return isInitialLoading ? <Spinner /> : <EventsGraph data={data} alerts={alerts} onZoom={onZoom} />;
};

export default EventsHistogram;
