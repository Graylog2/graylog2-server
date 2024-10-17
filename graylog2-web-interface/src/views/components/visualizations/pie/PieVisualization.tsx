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
import { useMemo } from 'react';

import type { VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import { makeVisualization, retrieveChartData } from 'views/components/aggregationbuilder/AggregationBuilder';
import PlotLegend from 'views/components/visualizations/PlotLegend';
import useChartData from 'views/components/visualizations/useChartData';
import type { ChartDefinition, Generator } from 'views/components/visualizations/ChartData';
import type ColorMapper from 'views/components/visualizations/ColorMapper';
import useMapKeys from 'views/components/visualizations/useMapKeys';
import type { KeyMapper } from 'views/components/visualizations/TransformKeys';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import { keySeparator, humanSeparator } from 'views/Constants';
import type {
  PieChartDataSettingsWithCustomUnits,
} from 'views/components/visualizations/hooks/usePieChartDataSettingsWithCustomUnits';
import usePieChartDataSettingsWithCustomUnits from 'views/components/visualizations/hooks/usePieChartDataSettingsWithCustomUnits';

import type { ChartConfig } from '../GenericPlot';
import GenericPlot from '../GenericPlot';

const maxItemsPerRow = 4;

const _verticalDimensions = (idx: number, total: number): [number, number] => {
  const rows = Math.ceil(total / maxItemsPerRow);
  const position = Math.floor(idx / maxItemsPerRow);

  const sliceSize = 1 / rows;
  const spacer = sliceSize * 0.1;

  return [(sliceSize * position) + spacer, (sliceSize * (position + 1)) - spacer];
};

const _horizontalDimensions = (idx: number, total: number): [number, number] => {
  const position = idx % maxItemsPerRow;

  const sliceSize = 1 / Math.min(total, maxItemsPerRow);
  const spacer = sliceSize * 0.1;

  return [(sliceSize * position) + spacer, (sliceSize * (position + 1)) - spacer];
};

const _generateSeries = (mapKeys: KeyMapper, getPieChartDataSettingsWithCustomUnits: PieChartDataSettingsWithCustomUnits): Generator => ({
  type,
  name,
  labels,
  values,
  idx,
  total,
  originalName,
  config,
  fullPath,
}): ChartDefinition => {
  const rowPivots = config?.rowPivots?.flatMap((pivot) => pivot.fields) ?? [];
  const extendedSettings = getPieChartDataSettingsWithCustomUnits({ values, name, fullPath });

  return {
    type,
    name,
    hole: 0.4,
    labels: labels.map((label) => label.split(keySeparator).map((l, i) => mapKeys(l, rowPivots[i])).join(humanSeparator)),
    originalLabels: labels,
    values,
    automargin: true,
    textposition: 'inside',
    domain: {
      x: _horizontalDimensions(idx, total),
      y: _verticalDimensions(idx, total),
    },
    originalName,
    ...extendedSettings,
  };
};

const setChartColor = (chart: ChartConfig, colorMap: ColorMapper) => {
  const colors = chart.originalLabels.map((label) => colorMap.get(label));

  return { marker: { colors } };
};

const labelMapper = (data: Array<{ labels: Array<string>, originalLabels?: Array<string> }>) => [
  ...new Set(data.flatMap(({ labels, originalLabels }) => originalLabels ?? labels)),
];

const rowPivotsToFields = (config: AggregationWidgetConfig) => config?.rowPivots?.flatMap((pivot) => pivot.fields);

const PieVisualization = makeVisualization(({ config, data, height, width }: VisualizationComponentProps) => {
  const rows = useMemo(() => retrieveChartData(data), [data]);
  const mapKeys = useMapKeys();
  const getPieChartDataSettingsWithCustomUnits = usePieChartDataSettingsWithCustomUnits({ config });
  const transformedData = useChartData(rows, { widgetConfig: config, chartType: 'pie', generator: _generateSeries(mapKeys, getPieChartDataSettingsWithCustomUnits) });

  return (
    <PlotLegend config={config} chartData={transformedData} labelMapper={labelMapper} labelFields={rowPivotsToFields} neverHide height={height} width={width}>
      <GenericPlot chartData={transformedData}
                   setChartColor={setChartColor} />
    </PlotLegend>
  );
}, 'pie');

PieVisualization.displayName = 'PieVisualization';

export default PieVisualization;
