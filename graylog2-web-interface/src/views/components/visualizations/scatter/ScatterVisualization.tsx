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
// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import EventHandler, { Shapes } from 'views/logic/searchtypes/events/EventHandler';
import { AggregationType, AggregationResult } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { VisualizationComponent, VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import { makeVisualization } from 'views/components/aggregationbuilder/AggregationBuilder';

import { chartData } from '../ChartData';
import XYPlot from '../XYPlot';

const seriesGenerator = (type, name, labels, values) => ({ type, name, x: labels, y: values, mode: 'markers' });

const ScatterVisualization: VisualizationComponent = makeVisualization(({ config, data, effectiveTimerange, height }: VisualizationComponentProps) => {
  const chartDataResult = chartData(config, data.chart || Object.values(data)[0], 'scatter', seriesGenerator);
  const layout: { shapes?: Shapes } = {};

  if (config.eventAnnotation && data.events) {
    const { eventChartData, shapes } = EventHandler.toVisualizationData(data.events, config.formattingSettings);

    chartDataResult.push(eventChartData);
    layout.shapes = shapes;
  }

  return (
    <XYPlot config={config}
            chartData={chartDataResult}
            plotLayout={layout}
            height={height}
            effectiveTimerange={effectiveTimerange} />
  );
}, 'scatter');

ScatterVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: AggregationResult.isRequired,
  height: PropTypes.number,
};

export default ScatterVisualization;
