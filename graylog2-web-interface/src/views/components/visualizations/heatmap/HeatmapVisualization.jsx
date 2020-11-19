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
import { values, merge, fill, find, isEmpty, get } from 'lodash';

import { AggregationType, AggregationResult } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { VisualizationComponent, VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import { makeVisualization } from 'views/components/aggregationbuilder/AggregationBuilder';

import type { ChartDefinition, ExtractedSeries } from '../ChartData';
import GenericPlot from '../GenericPlot';
import { chartData, type ValuesBySeries } from '../ChartData';

const BG_COLOR = '#440154';

const COLORSCALE = [
  [0.00, BG_COLOR],
  [0.05, '#481567'],
  [0.10, '#483677'],
  [0.15, '#453781'],
  [0.20, '#404788'],
  [0.30, '#39568c'],
  [0.35, '#33638d'],
  [0.40, '#2d708e'],
  [0.45, '#287d8e'],
  [0.50, '#238a8d'],
  [0.55, '#1f968b'],
  [0.60, '#20a387'],
  [0.65, '#29af7f'],
  [0.70, '#3cbb75'],
  [0.75, '#55c667'],
  [0.80, '#73d055'],
  [0.85, '#95d840'],
  [0.90, '#b8de29'],
  [0.95, '#dce319'],
  [1.00, '#fde725'],
];

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

const _generateSeries = (type, name, x, y, z, idx, total, config): ChartDefinition => {
  const xAxisTitle = get(config, ['rowPivots', idx, 'field']);
  const yAxisTitle = get(config, ['columnPivots', idx, 'field']);
  const zSeriesTitles = _generateSeriesTitles(config, y, x);
  const hovertemplate = `${xAxisTitle}: %{y}<br>${yAxisTitle}: %{x}<br>%{text}: %{customdata}<extra></extra>`;

  return {
    type,
    name,
    x: y,
    y: x,
    z,
    text: zSeriesTitles,
    customdata: z,
    hovertemplate,
    colorscale: COLORSCALE,
  };
};

const _fillUpMatrix = (z: Array<Array<any>>, xLabels: Array<any>) => {
  const defaultValue = 'None';

  return z.map((series) => {
    const newSeries = fill(Array(xLabels.length), defaultValue);

    return merge(newSeries, series);
  });
};

const _transposeMatrix = (z: Array<Array<any>> = []) => {
  if (!z[0]) { return z; }

  return z[0].map((_, c) => { return z.map((r) => { return r[c]; }); });
};

const _formatSeries = ({ valuesBySeries, xLabels }: {valuesBySeries: ValuesBySeries, xLabels: Array<any>}): ExtractedSeries => {
  // When using the hovertemplate, we need to provie a value for empty z values.
  // Otherwise plotly would throw errors when hovering over a field.
  // We need to transpose the z matrix, because we are changing the x and y label in the generator function
  const z = _transposeMatrix(_fillUpMatrix(values(valuesBySeries), xLabels));
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
    plot_bgcolor: hasContent ? BG_COLOR : 'transparent',
    margin: {
      b: 40,
    },
  };
};

const _leafSourceMatcher = ({ source }) => source.endsWith('leaf') && source !== 'row-leaf';

const HeatmapVisualization: VisualizationComponent = makeVisualization(({ config, data }: VisualizationComponentProps) => {
  const rows = data.chart || Object.values(data)[0];
  const heatmapData = chartData(config, rows, 'heatmap', _generateSeries, _formatSeries, _leafSourceMatcher);
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
