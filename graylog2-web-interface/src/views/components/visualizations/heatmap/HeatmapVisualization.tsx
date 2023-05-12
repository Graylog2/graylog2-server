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
import _values from 'lodash/values';
import merge from 'lodash/merge';
import fill from 'lodash/fill';
import find from 'lodash/find';
import isEmpty from 'lodash/isEmpty';

import { AggregationType, AggregationResult } from 'views/components/aggregationbuilder/AggregationBuilderPropTypes';
import type { VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import { makeVisualization, retrieveChartData } from 'views/components/aggregationbuilder/AggregationBuilder';
import HeatmapVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/HeatmapVisualizationConfig';
import useChartData from 'views/components/visualizations/useChartData';
import type { KeyMapper } from 'views/components/visualizations/TransformKeys';
import useMapKeys from 'views/components/visualizations/useMapKeys';

import type { ChartDefinition, ExtractedSeries, ValuesBySeries, Generator } from '../ChartData';
import GenericPlot from '../GenericPlot';

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

const _generateSeries = (visualizationConfig: HeatmapVisualizationConfig, mapKeys: KeyMapper): Generator => ({
  type,
  name,
  labels,
  values,
  data: z,
  config,
}): ChartDefinition => {
  const rowPivots = config.rowPivots.flatMap((pivot) => pivot.fields);
  const columnPivots = config.columnPivots.flatMap((pivot) => pivot.fields);
  const xAxisTitle = rowPivots.join('-');
  const yAxisTitle = columnPivots.join('-');
  const zSeriesTitles = _generateSeriesTitles(config, values, labels);
  const hovertemplate = `${xAxisTitle}: %{y}<br>${yAxisTitle}: %{x}<br>%{text}: %{customdata}<extra></extra>`;
  const { colorScale, reverseScale, zMin, zMax } = visualizationConfig;
  const y = labels.map((value) => mapKeys(value, rowPivots[0]));
  const x = values.map((value) => mapKeys(value, columnPivots[0]));

  return {
    type,
    name,
    x,
    y,
    z,
    text: zSeriesTitles,
    customdata: z,
    hovertemplate,
    colorscale: colorScale,
    reversescale: reverseScale,
    zmin: zMin,
    zmax: zMax,
    originalName: name,
  };
};

const _fillUpMatrix = (z: Array<Array<any>>, xLabels: Array<any>, defaultValue: number | 'None' = 'None') => z.map((series) => {
  const newSeries = fill(Array(xLabels.length), defaultValue);

  return merge(newSeries, series);
});

const _transposeMatrix = (z: Array<Array<any>> = []) => {
  if (!z[0]) {
    return z;
  }

  return z[0].map((_, c) => z.map((r) => r[c]));
};

const _findSmallestValue = (valuesFound: Array<Array<number>>) => valuesFound.reduce((result, valueArray) => valueArray.reduce((acc, value) => (acc > value ? value : acc), result), (valuesFound[0] || [])[0]);

const _formatSeries = (visualizationConfig: HeatmapVisualizationConfig) => ({
  valuesBySeries,
  xLabels,
}: { valuesBySeries: ValuesBySeries, xLabels: Array<any> }): ExtractedSeries => {
  const valuesFoundBySeries = _values(valuesBySeries);
  // When using the hovertemplate, we need to provide a value for empty z values.
  // Otherwise, plotly would throw errors when hovering over a field.
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

const _axisConfig = (chartHasContent: ChartDefinition) => {
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

const _chartLayout = (heatmapData: ChartDefinition[]) => {
  const hasContent = find(heatmapData, (series) => !isEmpty(series.z));
  const axisConfig = _axisConfig(hasContent);

  return {
    yaxis: axisConfig,
    xaxis: axisConfig,
    margin: {
      b: 40,
    },
  };
};

const _leafSourceMatcher = ({ source }: { source: string }) => source.endsWith('leaf') && source !== 'row-leaf';

const HeatmapVisualization = makeVisualization(({ config, data }: VisualizationComponentProps) => {
  const visualizationConfig = (config.visualizationConfig ?? HeatmapVisualizationConfig.empty()) as HeatmapVisualizationConfig;
  const rows = retrieveChartData(data);
  const mapKeys = useMapKeys();
  const heatmapData = useChartData(rows, {
    widgetConfig: config,
    chartType: 'heatmap',
    generator: _generateSeries(visualizationConfig, mapKeys),
    seriesFormatter: _formatSeries(visualizationConfig),
    leafValueMatcher: _leafSourceMatcher,
  });
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
