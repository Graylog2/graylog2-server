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
import { useCallback, useEffect, useMemo } from 'react';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import styled from 'styled-components';
import merge from 'lodash/merge';

import { Alert } from 'components/bootstrap';
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
import type { UserDateTimeContextType } from 'contexts/UserDateTimeContext';
import useOnRefresh from 'components/common/PaginatedEntityTable/useOnRefresh';

const config = AggregationWidgetConfig.builder()
  .visualization('area')
  .rowPivots([Pivot.create(['timestamp'], DateType)])
  .columnPivots([Pivot.createValues(['type'])])
  .series([Series.forFunction('count()')])
  .rollup(false)
  .build();

const DEFAULT_HEIGHT = '180px';

const GraphContainer = styled.div<{ $height: string }>`
  height: ${({ $height }) => $height};
  width: 100%;
`;

type ResultPromise = ReturnType<typeof fetchEventsHistogram>;

type FormatTime = UserDateTimeContextType['formatTime'];

const generateChart = (
  type: 'Alerts' | 'Events',
  buckets: Awaited<ResultPromise>['results']['buckets']['alerts' | 'events'],
  formatTime: FormatTime,
) => {
  const x = buckets.map((b) => formatTime(b.start_date, 'internal'));
  const y = buckets.map((b) => b.count);

  return {
    type: 'scatter',
    name: type,
    x,
    y,
    fill: 'tozeroy',
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
};

export type EffectiveTimeRange = {
  from: string;
  to: string;
  type: string;
};

const prepareTimeRangeForGraph = (timerange: EffectiveTimeRange, formatTime: FormatTime) => [
  formatTime(timerange.from, 'internal'),
  formatTime(timerange.to, 'internal'),
];

const EventsGraph = ({
  data: { results, timerange },
  alerts,
  onZoom,
  formatTime,
  height,
  readOnly,
}: {
  data: Awaited<ResultPromise>;
  alerts: 'include' | 'exclude' | 'only';
  onZoom: (from: string, to: string) => void;
  formatTime: FormatTime;
  height: string;
  readOnly: boolean;
}) => {
  const chartData = useMemo(
    () => [
      ...(['include', 'exclude'].includes(alerts) ? [generateChart('Events', results.buckets.events, formatTime)] : []),
      ...(['include', 'only'].includes(alerts) ? [generateChart('Alerts', results.buckets.alerts, formatTime)] : []),
    ],
    [alerts, results.buckets.alerts, results.buckets.events, formatTime],
  );

  const baseLayout = useMemo(
    () => ({
      ...layout,
      xaxis: {
        ...layout.xaxis,
        range: prepareTimeRangeForGraph(timerange, formatTime),
      },
    }),
    [timerange, formatTime],
  );

  return (
    <GraphContainer $height={height}>
      <InteractiveContext.Provider value="disabled">
        <FullSizeContainer>
          {(dimensions) => (
            <PlotLegend config={config} chartData={chartData} height={dimensions.height} width={dimensions.width}>
              <InteractiveContext.Provider value={readOnly ? 'read-only' : 'interactive'}>
                <GenericPlot
                  chartData={chartData}
                  layout={merge({}, baseLayout, { legend: { y: yLegendPosition(dimensions.height) } })}
                  onZoom={onZoom}
                  setChartColor={defaultSetColor}
                />
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
  height?: string;
  readOnly?: boolean;
  onEffectiveTimeRangeChange?: (timerange: EffectiveTimeRange) => void;
};
const EventsHistogram = ({
  searchParams,
  setFilters,
  eventsHistogramFetcher = fetchEventsHistogram,
  height = DEFAULT_HEIGHT,
  readOnly = false,
  onEffectiveTimeRangeChange = undefined,
}: Props) => {
  const { userTimezone, formatTime } = useUserDateTime();
  const { data, isLoading, refetch, isError, error } = useQuery({
    queryKey: ['events', 'histogram', searchParams],
    queryFn: () => eventsHistogramFetcher(searchParams),
    placeholderData: keepPreviousData,
  });

  useOnRefresh(refetch);

  useEffect(() => {
    if (data) {
      onEffectiveTimeRangeChange?.(data.timerange);
    }
  }, [data, onEffectiveTimeRangeChange]);

  const alerts = parseTypeFilter(searchParams?.filters?.get('alert')?.[0]);
  const onZoom = useCallback(
    (from: string, to: string) => {
      const parsedFrom = formatTime(toUTCFromTz(from, userTimezone), 'internal');
      const parsedTo = formatTime(toUTCFromTz(to, userTimezone), 'internal');
      setFilters(searchParams.filters.set('timestamp', [`${parsedFrom}><${parsedTo}`]));
    },
    [formatTime, searchParams.filters, setFilters, userTimezone],
  );

  if (isLoading) {
    return <Spinner />;
  }

  if (isError || !data) {
    return <Alert bsStyle="danger">Loading events histogram failed: {error?.message ?? 'Unknown error'}</Alert>;
  }

  return (
    <EventsGraph
      data={data}
      alerts={alerts}
      onZoom={onZoom}
      formatTime={formatTime}
      height={height}
      readOnly={readOnly}
    />
  );
};

export default EventsHistogram;
