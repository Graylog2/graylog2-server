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
import PropTypes from 'prop-types';

import { AggregationType, AggregationResult } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import { makeVisualization, retrieveChartData } from 'views/components/aggregationbuilder/AggregationBuilder';
import type { Shapes } from 'views/logic/searchtypes/events/EventHandler';
import { DateType } from 'views/logic/aggregationbuilder/Pivot';
import type BarVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/BarVisualizationConfig';
import useChartData from 'views/components/visualizations/useChartData';
import useEvents from 'views/components/visualizations/useEvents';

import type { Generator } from '../ChartData';
import XYPlot from '../XYPlot';

type ChartDefinition = {
  type: string,
  name: string,
  x?: Array<string>,
  y?: Array<any>,
  z?: Array<Array<any>>,
  opacity: number,
};

const getChartColor = (fullData, name) => {
  const data = fullData.find((d) => (d.name === name)).marker;

  if (data && data.marker && data.marker.color) {
    const { marker: color } = data;

    return color;
  }

  return undefined;
};

const setChartColor = (chart, colors) => ({ marker: { color: colors.get(chart.name) } });

const defineSingleDateBarWidth = (chartDataResult, config, timeRangeFrom: string, timeRangeTo: string) => {
  const barWidth = 0.03; // width in percentage, relative to chart width
  const minXUnits = 30;

  if (config.rowPivots.length !== 1 || config.rowPivots[0].type !== DateType) {
    return chartDataResult;
  }

  return chartDataResult.map((data) => {
    if (data?.x?.length === 1) {
      // @ts-ignore
      const timeRangeMS = new Date(timeRangeTo) - new Date(timeRangeFrom);
      const widthXUnits = timeRangeMS * barWidth;

      return {
        ...data,
        width: [Math.max(minXUnits, widthXUnits)],
      };
    }

    return data;
  });
};

type Layout = {
  shapes?: Shapes;
  barmode?: string;
};

const BarVisualization = makeVisualization(({
  config,
  data,
  effectiveTimerange,
  height,
}: VisualizationComponentProps) => {
  const visualizationConfig = config.visualizationConfig as BarVisualizationConfig;
  const _layout: Layout = {};

  if (visualizationConfig && visualizationConfig.barmode) {
    _layout.barmode = visualizationConfig?.barmode;
  }

  const opacity = visualizationConfig?.opacity ?? 1.0;

  const _seriesGenerator: Generator = useCallback((type, name, labels, values): ChartDefinition => ({
    type,
    name,
    x: labels,
    y: values,
    opacity,
  }), [opacity]);

  const rows = useMemo(() => retrieveChartData(data), [data]);
  const _chartDataResult = useChartData(rows, { widgetConfig: config, chartType: 'bar', generator: _seriesGenerator });

  const { eventChartData, shapes } = useEvents(config, data.events);

  const chartDataResult = eventChartData ? [..._chartDataResult, eventChartData] : _chartDataResult;
  const layout = shapes ? { ..._layout, shapes } : _layout;

  return (
    <XYPlot config={config}
            chartData={defineSingleDateBarWidth(chartDataResult, config, effectiveTimerange?.from, effectiveTimerange?.to)}
            effectiveTimerange={effectiveTimerange}
            getChartColor={getChartColor}
            height={height}
            setChartColor={setChartColor}
            plotLayout={layout} />
  );
}, 'bar');

BarVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: AggregationResult.isRequired,
  height: PropTypes.number,
};

export default BarVisualization;
