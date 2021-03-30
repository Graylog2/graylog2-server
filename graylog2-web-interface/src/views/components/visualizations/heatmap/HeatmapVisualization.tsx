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
import { values, merge, fill, find, isEmpty, get } from 'lodash';

import { AggregationType, AggregationResult } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import { makeVisualization } from 'views/components/aggregationbuilder/AggregationBuilder';
import HeatmapVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/HeatmapVisualizationConfig';

import type { ChartDefinition, ExtractedSeries } from '../ChartData';
import GenericPlot from '../GenericPlot';
import { chartData, ValuesBySeries } from '../ChartData';

const _generateSeriesTitles = (config, x, y) => {
  const seriesTitles = config.series.map((s) => s.function);
  const columnSeriesTitles = x.map((xLabel) => {
    if (seriesTitles.length > 1) {
      return seriesTitles.find((title) => xLabel.endsWith(title));
    }

    return seriesTitles.toString();
  });

  return y.map(() => columnSeriesTitles);
};

const _heatmapGenerateSeries = (type, name, x, y, z, idx, total, config, visualizationConfig): ChartDefinition => {
  const xAxisTitle = get(config, ['rowPivots', idx, 'field']);
  const yAxisTitle = get(config, ['columnPivots', idx, 'field']);
  const zSeriesTitles = _generateSeriesTitles(config, y, x);
  const hovertemplate = `${xAxisTitle}: %{y}<br>${yAxisTitle}: %{x}<br>%{text}: %{customdata}<extra></extra>`;
  const { colorScale, reverseScale, zMin, zMax } = visualizationConfig;

  return {
    type,
    name,
    x: y,
    y: x,
    z,
    text: zSeriesTitles,
    customdata: z,
    hovertemplate,
    colorscale: colorScale,
    reversescale: reverseScale,
    zmin: zMin,
    zmax: zMax,
  };
};

const _generateSeries = (visualizationConfig) => (type, name, x, y, z, idx, total, config) => _heatmapGenerateSeries(type, name, x, y, z, idx, total, config, visualizationConfig);

const _fillUpMatrix = (z: Array<Array<any>>, xLabels: Array<any>, defaultValue = 'None') => {
  return z.map((series) => {
    const newSeries = fill(Array(xLabels.length), defaultValue);

    return merge(newSeries, series);
  });
};

const _transposeMatrix = (z: Array<Array<any>> = []) => {
  if (!z[0]) { return z; }

  return z[0].map((_, c) => { return z.map((r) => { return r[c]; }); });
};

const _findSmallestValue = (valuesFound: Array<Array<number>>) => valuesFound.reduce((result, valueArray) => valueArray.reduce((acc, value) => (acc > value ? value : acc), result), valuesFound[0][0]);

const _formatSeries = (visualizationConfig) => ({ valuesBySeries, xLabels }: {valuesBySeries: ValuesBySeries, xLabels: Array<any>}): ExtractedSeries => {
  const valuesFoundBySeries = values(valuesBySeries);
  // When using the hovertemplate, we need to provie a value for empty z values.
  // Otherwise plotly would throw errors when hovering over a field.
  // We need to transpose the z matrix, because we are changing the x and y label in the generator function
  const defaultValue = visualizationConfig.useSmallestAsDefault
    ? _findSmallestValue(valuesFoundBySeries)
    : (visualizationConfig.defaultValue ?? 'None');
  const z = _transposeMatrix(_fillUpMatrix(valuesFoundBySeries, xLabels, defaultValue));
  const yLabels = Object.keys(valuesBySeries);

  return [[
    'Heatmap Chart',
    xLabels,
    yLabels,
    z,
  ]];
};

const _axisConfg = (chartHasContent) => {
  const axisConfig = {
    type: undefined,
    fixedrange: true,
  };

  // Adding the axis type, without provided z data, would hide the empty grid
  if (chartHasContent) {
    axisConfig.type = 'category';
  }

  return axisConfig;
};

const _chartLayout = (heatmapData) => {
  const hasContent = find(heatmapData, (series) => !isEmpty(series.z));
  const axisConfig = _axisConfg(hasContent);

  return {
    yaxis: axisConfig,
    xaxis: axisConfig,
    margin: {
      b: 40,
    },
  };
};

const _leafSourceMatcher = ({ source }) => source.endsWith('leaf') && source !== 'row-leaf';

const HeatmapVisualization = makeVisualization(({ config, data }: VisualizationComponentProps) => {
  const visualizationConfig = (config.visualizationConfig || HeatmapVisualizationConfig.empty()) as HeatmapVisualizationConfig;
  const rows = data.chart || Object.values(data)[0];
  const heatmapData = chartData(config, rows, 'heatmap', _generateSeries(visualizationConfig), _formatSeries(visualizationConfig), _leafSourceMatcher);
  const layout = _chartLayout(heatmapData);

  return (
    <GenericPlot chartData={heatmapData} layout={layout} />
  );
}, 'heatmap');

HeatmapVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: AggregationResult.isRequired,
};

export default HeatmapVisualization;
