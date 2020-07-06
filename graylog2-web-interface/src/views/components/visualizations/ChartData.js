// @flow strict
import { flatten, flow, isEqual, set } from 'lodash';

import type { Key, Leaf, Rows, Value } from 'views/logic/searchtypes/pivot/PivotHandler';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';

import transformKeys from './TransformKeys';

export type ChartDefinition = {
  type: string,
  name: string,
  x?: Array<string>,
  y?: Array<any>,
  z?: Array<Array<any>>,
};

export type ChartData = [any, Array<Key>, Array<any>, Array<Array<any>>];
export type ExtractedSeries = Array<ChartData>;
export type ValuesBySeries = { [string]: Array<number>};

export type KeyJoiner = (Array<any>) => string;

export type Generator = (string, string, Array<string>, Array<any>, Array<Array<any>>, number, number, AggregationWidgetConfig) => ChartDefinition;

const _defaultKeyJoiner = (keys) => keys.join('-');

const _defaultChartGenerator = (type, name, labels, values): ChartDefinition => ({ type, name, x: labels, y: values });

export const flattenLeafs = (leafs: Array<Leaf>, matcher: Value => boolean = ({ source }) => source.endsWith('leaf')): Array<any> => {
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

export const getLeafsFromRows = (rows: Rows): Array<Leaf> => {
  // $FlowFixMe: Somehow flow is unable to infer that the result consists only of Leafs.
  return rows.filter((row) => (row.source === 'leaf'));
};

export const getXLabelsFromLeafs = (leafs: Array<Leaf>): Array<Array<Key>> => leafs.map(({ key }) => key);

export const extractSeries = (keyJoiner: KeyJoiner = _defaultKeyJoiner, leafValueMatcher?: Value => boolean) => {
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

export const generateChart = (chartType: string, generator: Generator = _defaultChartGenerator, config: AggregationWidgetConfig): ((ExtractedSeries) => Array<ChartDefinition>) => {
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
  customSeriesFormatter?: ({valuesBySeries: ValuesBySeries, xLabels: Array<any>}) => ExtractedSeries = formatSeries,
  leafValueMatcher?: Value => boolean,
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
