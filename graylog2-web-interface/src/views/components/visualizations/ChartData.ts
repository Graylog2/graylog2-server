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
import flatten from 'lodash/flatten';
import flow from 'lodash/flow';
import isEqual from 'lodash/isEqual';
import set from 'lodash/set';

import type { Key, Leaf, Row, Rows, Value } from 'views/logic/searchtypes/pivot/PivotHandler';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type { DateTime, DateTimeFormats } from 'util/DateTime';

import type { KeyMapper } from './TransformKeys';
import transformKeys from './TransformKeys';

const keySeparator = '\u2E31';
const humanSeparator = '-';

export type ChartDefinition = {
  type: string,
  name: string,
  x?: Array<string>,
  y?: Array<any>,
  z?: Array<Array<any>>,
  fill?: string,
  line?: { shape: string },
  hovertemplate?: string,
  mode?: string,
  opacity?: number,
  text?: string[],
  marker?: {
    size: number,
    color: string,
  },
  customdata?: any,
  colorscale?: string,
  reversescale?: boolean,
  zmin?: number,
  zmax?: number,
  originalName: string,
};

export type ChartData = [any, Array<Key>, Array<any>, Array<Array<any>>];
export type ExtractedSeries = Array<ChartData>;
export type ValuesBySeries = { [key: string]: Array<number> };

export type KeyJoiner = (keys: Array<any>) => string;

type ChartInput = {
  type: string,
  name: string,
  originalName: string,
  labels: Array<string>,
  values: Array<any>,
  data: Array<Array<any>>,
  idx: number,
  total: number,
  config: AggregationWidgetConfig
};
export type Generator = (chartInput: ChartInput) => ChartDefinition;

const _defaultKeyJoiner = (keys: Array<any>) => keys.join(keySeparator);

const _defaultChartGenerator = ({ type, name, labels, values, originalName }: ChartInput): ChartDefinition => ({
  type,
  name,
  x: labels,
  y: values,
  originalName,
});

export const flattenLeafs = (leafs: Array<Leaf>, matcher: (value: Value) => boolean = ({ source }) => source.endsWith('leaf')): Array<any> => flatten(leafs.map((l) => l.values.filter((value) => matcher(value)).map((v) => [l.key, v])));

export const formatSeries = ({
  valuesBySeries = {},
  xLabels = [],
}: { valuesBySeries: ValuesBySeries, xLabels: Array<any> }): ExtractedSeries => Object.keys(valuesBySeries).map((value) => [
  value,
  xLabels,
  valuesBySeries[value],
  [],
]);

const isLeaf = (row: Row): row is Leaf => (row.source === 'leaf');

export const getLeafsFromRows = (rows: Rows): Array<Leaf> => rows.filter(isLeaf);

export const getXLabelsFromLeafs = (leafs: Array<Leaf>): Array<Array<Key>> => leafs.map(({ key }) => key);

export const extractSeries = (keyJoiner: KeyJoiner = _defaultKeyJoiner, leafValueMatcher: (value: Value) => boolean = undefined) => (results: Rows) => {
  const leafs = getLeafsFromRows(results);
  const xLabels = getXLabelsFromLeafs(leafs);
  const flatLeafs = flattenLeafs(leafs, leafValueMatcher);
  const valuesBySeries = {};

  flatLeafs.forEach(([key, value]) => {
    const joinedKey = keyJoiner(value.key);
    const targetIdx = xLabels.findIndex((l) => isEqual(l, key));

    if (value.value !== null && value.value !== undefined) {
      set(valuesBySeries, [joinedKey, targetIdx], value.value);
    }
  });

  return { valuesBySeries, xLabels };
};

export const generateChart = (
  chartType: string,
  generator: Generator = _defaultChartGenerator,
  config: AggregationWidgetConfig = undefined,
  mapKeys: KeyMapper = (key) => key,
): ((results: ExtractedSeries) => Array<ChartDefinition>) => {
  const columnFields = config.columnPivots.flatMap((pivot) => pivot.fields);

  return (results: ExtractedSeries) => {
    const allCharts = results.map(([value, x, values, z]) => ({
      type: chartType,
      name: value.split(keySeparator).map((key, idx) => (columnFields[idx] ? mapKeys(key, columnFields[idx]) : key)).join(humanSeparator),
      labels: x.map((key) => key.join(keySeparator)),
      values,
      data: z,
      originalName: value,
    }));

    return allCharts.map((args, idx) => generator({ ...args, idx, total: allCharts.length, config }));
  };
};

export const removeNulls = (): ((series: ExtractedSeries) => ExtractedSeries) => (results: ExtractedSeries) => results.map(([name, keys, values, z]) => {
  const nullIndices = Array.from(values).reduce((indices, value, index) => ((value === null || value === undefined) ? [...indices, index] : indices), []);
  const newKeys = keys.filter((_, idx) => !nullIndices.includes(idx));
  const newValues = values.filter((_, idx) => !nullIndices.includes(idx));

  return [name, newKeys, newValues, z];
});

const doNotSuffixTraceForSingleSeries = (keys: Array<Key>) => (keys.length > 1 ? keys.slice(0, -1).join(keySeparator) : keys[0]);

export type ChartDataConfig = {
  widgetConfig: AggregationWidgetConfig,
  chartType: string,
  generator?: Generator,
  seriesFormatter?: (values: { valuesBySeries: ValuesBySeries, xLabels: Array<any> }) => ExtractedSeries,
  leafValueMatcher?: (value: Value) => boolean,
  formatTime: (time: DateTime, format?: DateTimeFormats) => string,
  mapKeys?: (key: Key, field: string) => Key,
};

export const chartData = (
  data: Rows,
  {
    chartType,
    widgetConfig: config,
    generator = _defaultChartGenerator,
    seriesFormatter: customSeriesFormatter = formatSeries,
    leafValueMatcher,
    formatTime,
    mapKeys,
  }: ChartDataConfig,
): Array<ChartDefinition> => {
  const { rowPivots, columnPivots, series } = config;

  return flow([
    transformKeys(rowPivots, columnPivots, formatTime),
    extractSeries(series.length === 1 ? doNotSuffixTraceForSingleSeries : undefined, leafValueMatcher),
    customSeriesFormatter,
    removeNulls(),
    generateChart(chartType, generator, config, mapKeys),
  ])(data);
};
