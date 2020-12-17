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
import { flatten, flow, isEqual, set } from 'lodash';

import type { Key, Leaf, Row, Rows, Value } from 'views/logic/searchtypes/pivot/PivotHandler';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';

import transformKeys from './TransformKeys';

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
  colorscale?: [number, string][],
  reversescale?: boolean,
  zmin?: boolean,
  zmax?: boolean,
};

export type ChartData = [any, Array<Key>, Array<any>, Array<Array<any>>];
export type ExtractedSeries = Array<ChartData>;
export type ValuesBySeries = { [key: string]: Array<number>};

export type KeyJoiner = (keys: Array<any>) => string;

export type Generator = (type: string, name: string, labels: Array<string>, values: Array<any>, data: Array<Array<any>>, idx: number, total: number, config: AggregationWidgetConfig) => ChartDefinition;

const _defaultKeyJoiner = (keys) => keys.join('-');

const _defaultChartGenerator = (type, name, labels, values): ChartDefinition => ({ type, name, x: labels, y: values });

export const flattenLeafs = (leafs: Array<Leaf>, matcher: (value: Value) => boolean = ({ source }) => source.endsWith('leaf')): Array<any> => {
  return flatten(leafs.map((l) => l.values.filter((value) => matcher(value)).map((v) => [l.key, v])));
};

export const formatSeries = ({ valuesBySeries = {}, xLabels = [] }: {valuesBySeries: ValuesBySeries, xLabels: Array<any>}): ExtractedSeries => {
  return Object.keys(valuesBySeries).map((value) => [
    value,
    xLabels,
    valuesBySeries[value],
    [],
  ]);
};

const isLeaf = (row: Row): row is Leaf => (row.source === 'leaf');

export const getLeafsFromRows = (rows: Rows): Array<Leaf> => {
  return rows.filter(isLeaf);
};

export const getXLabelsFromLeafs = (leafs: Array<Leaf>): Array<Array<Key>> => leafs.map(({ key }) => key);

export const extractSeries = (keyJoiner: KeyJoiner = _defaultKeyJoiner, leafValueMatcher?: (value: Value) => boolean) => {
  return (results: Rows) => {
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
};

export const generateChart = (chartType: string, generator: Generator = _defaultChartGenerator, config?: AggregationWidgetConfig): ((ExtractedSeries) => Array<ChartDefinition>) => {
  return (results: ExtractedSeries) => {
    const allCharts: Array<[string, string, Array<string>, Array<any>, Array<Array<any>>]> = results.map(([value, x, values, z]) => [
      chartType,
      value,
      x.map((key) => key.join('-')),
      values,
      z,
    ]);

    return allCharts.map((args, idx) => generator(...args, idx, allCharts.length, config));
  };
};

export const removeNulls = (): ((ExtractedSeries) => ExtractedSeries) => {
  return (results: ExtractedSeries) => results.map(([name, keys, values, z]) => {
    const nullIndices = Array.from(values).reduce((indices, value, index) => ((value === null || value === undefined) ? [...indices, index] : indices), []);
    const newKeys = keys.filter((_, idx) => !nullIndices.includes(idx));
    const newValues = values.filter((_, idx) => !nullIndices.includes(idx));

    return [name, newKeys, newValues, z];
  });
};

const doNotSuffixTraceForSingleSeries = (keys) => (keys.length > 1 ? keys.slice(0, -1).join('-') : keys[0]);

export const chartData = (
  config: AggregationWidgetConfig,
  data: Rows,
  chartType: string,
  generator: Generator = _defaultChartGenerator,
  customSeriesFormatter: (values: { valuesBySeries: ValuesBySeries, xLabels: Array<any> }) => ExtractedSeries = formatSeries,
  leafValueMatcher?: (value: Value) => boolean,
): Array<ChartDefinition> => {
  const { rowPivots, columnPivots, series } = config;

  return flow([
    transformKeys(rowPivots, columnPivots),
    extractSeries(series.length === 1 ? doNotSuffixTraceForSingleSeries : undefined, leafValueMatcher),
    customSeriesFormatter,
    removeNulls(),
    generateChart(chartType, generator, config),
  ])(data);
};
