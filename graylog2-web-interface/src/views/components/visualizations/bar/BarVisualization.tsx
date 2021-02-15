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
import PropTypes from 'prop-types';

import { AggregationType, AggregationResult } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { VisualizationComponent, VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import EventHandler, { Shapes } from 'views/logic/searchtypes/events/EventHandler';
import { DateType } from 'views/logic/aggregationbuilder/Pivot';
import { makeVisualization } from 'views/components/aggregationbuilder/AggregationBuilder';
import BarVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/BarVisualizationConfig';

import { chartData } from '../ChartData';
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

const BarVisualization: VisualizationComponent = makeVisualization(({ config, data, effectiveTimerange, height }: VisualizationComponentProps) => {
  const visualizationConfig = config.visualizationConfig as BarVisualizationConfig;
  const layout: Layout = {};

  if (visualizationConfig && visualizationConfig.barmode) {
    layout.barmode = visualizationConfig.barmode;
  }

  const opacity = visualizationConfig ? visualizationConfig.opacity : 1.0;

  const _seriesGenerator = (type, name, labels, values): ChartDefinition => ({ type, name, x: labels, y: values, opacity });

  const rows = data.chart || Object.values(data)[0];
  const chartDataResult = chartData(config, rows, 'bar', _seriesGenerator);

  if (config.eventAnnotation && data.events) {
    const { eventChartData, shapes } = EventHandler.toVisualizationData(data.events, config.formattingSettings);

    chartDataResult.push(eventChartData);
    layout.shapes = shapes;
  }

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
