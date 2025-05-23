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

import React, { useCallback } from 'react';
import merge from 'lodash/merge';
import cloneDeep from 'lodash/cloneDeep';
import moment from 'moment';

import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type ColorMapper from 'views/components/visualizations/ColorMapper';
import PlotLegend from 'views/components/visualizations/PlotLegend';
import useUserDateTime from 'hooks/useUserDateTime';
import type { AxisType } from 'views/logic/aggregationbuilder/visualizations/XYVisualization';
import { DEFAULT_AXIS_TYPE } from 'views/logic/aggregationbuilder/visualizations/XYVisualization';
import assertUnreachable from 'logic/assertUnreachable';
import useViewsDispatch from 'views/stores/useViewsDispatch';

import GenericPlot from './GenericPlot';
import type { ChartConfig, PlotLayout } from './GenericPlot';
import OnZoom from './OnZoom';

type GenericPlotProps = React.ComponentProps<typeof GenericPlot>;
export type Props = {
  axisType?: AxisType;
  config: AggregationWidgetConfig;
  chartData: any;
  effectiveTimerange?: {
    from: string;
    to: string;
  };
  height: number;
  width: number;
  setChartColor?: GenericPlotProps['setChartColor'];
  plotLayout?: Partial<PlotLayout>;
  onZoom?: (from: string, to: string, userTimezone: string) => boolean;
  onClickMarker?: GenericPlotProps['onClickMarker'];
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

const mapAxisType = (axisType: AxisType): 'linear' | 'log' => {
  switch (axisType) {
    case 'linear':
      return 'linear';
    case 'logarithmic':
      return 'log';
    default:
      return assertUnreachable(axisType, 'Unable to parse axis type: ');
  }
};

const defaultSetColor = (chart: ChartConfig, colors: ColorMapper) => ({
  line: { color: colors.get(chart.originalName ?? chart.name) },
});

type RangeProps = { minX: string; maxX: string };
const adjustRangeWithStep = ({ minX, maxX }: RangeProps, itemsLength: number, chartsLength: number): RangeProps => {
  const minMoment = moment(minX);
  const maxMoment = moment(maxX);

  const durationMs = maxMoment.diff(minMoment);
  const stepDurationMs = durationMs / itemsLength;

  const delta = 1 / (2 * chartsLength);
  console.log({ delta, stepDurationMs });
  const adjustedMinX = minMoment.clone().subtract(stepDurationMs * delta, 'milliseconds');
  const adjustedMaxX = maxMoment.clone().add(stepDurationMs * (1 - delta), 'milliseconds');

  return {
    minX: adjustedMinX.format(),
    maxX: adjustedMaxX.format(),
  };
};

const findDateRange = (chartDataArray: Array<{ x: Array<string> }>): RangeProps => {
  // Flatten all date strings from the 'x' arrays
  const allDates = chartDataArray.flatMap((data) => data.x || []);

  if (allDates.length === 0) {
    throw new Error('No date values found in the ChartData array');
  }

  let earliest = moment(allDates[0]);
  let latest = moment(allDates[0]);

  allDates.forEach((dateStr) => {
    const current = moment(dateStr);
    if (current.isBefore(earliest)) earliest = current;
    if (current.isAfter(latest)) latest = current;
  });

  return {
    minX: earliest.format(), // keeps original timezone
    maxX: latest.format(),
  };
};

const XYPlot = ({
  axisType = DEFAULT_AXIS_TYPE,
  config,
  chartData,
  effectiveTimerange = undefined,
  setChartColor = defaultSetColor,
  height,
  width,
  plotLayout = {},
  onZoom = undefined,
  onClickMarker = undefined,
}: Props) => {
  const { userTimezone } = useUserDateTime();
  const yaxis = { fixedrange: true, rangemode: 'tozero', tickformat: ',~r', type: mapAxisType(axisType) } as const;
  const defaultLayout: Partial<PlotLayout> = {
    yaxis,
    hovermode: 'x',
  };

  if (height) {
    defaultLayout.legend = { y: yLegendPosition(height) };
  }

  const layout: Partial<PlotLayout> = cloneDeep({ ...defaultLayout, ...plotLayout });
  const dispatch = useViewsDispatch();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const _onZoom = useCallback(
    config.isTimeline
      ? (from: string, to: string) =>
          onZoom ? onZoom(from, to, userTimezone) : dispatch(OnZoom(from, to, userTimezone))
      : () => true,
    [config.isTimeline, onZoom],
  );

  if (config.isTimeline && effectiveTimerange) {
    console.log({ chartData });
    const xValues = chartData?.[0]?.x;
    const { minX, maxX } = adjustRangeWithStep(findDateRange(chartData), xValues.length, chartData.length);
    layout.xaxis = merge(layout.xaxis, {
      range: [minX, maxX],
      type: 'date',
    });
  } else {
    layout.xaxis = merge(layout.xaxis, {
      fixedrange: true,
      /* disable plotly sorting by setting the type of the xaxis to category */
      type: config.sort.length > 0 ? 'category' : undefined,
    });
  }
  console.log({ layout });

  return (
    <PlotLegend config={config} chartData={chartData} height={height} width={width}>
      <GenericPlot
        chartData={chartData}
        layout={layout}
        onZoom={_onZoom}
        setChartColor={setChartColor}
        onClickMarker={onClickMarker}
      />
    </PlotLegend>
  );
};

export default XYPlot;
