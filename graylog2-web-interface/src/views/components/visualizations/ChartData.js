// @flow strict
import { flatten, flow, isEqual, set } from 'lodash';

import type { Key, Leaf, Rows } from 'views/logic/searchtypes/pivot/PivotHandler';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import transformKeys from './TransformKeys';

export type ChartDefinition = {
  type: string,
  name: string,
  x?: Array<string>,
  y?: Array<any>,
  z?: Array<any>,
};

export type ChartData = [any, Array<Key>, Array<any>, Array<any>]
export type ExtractedSeries = Array<ChartData>;

export type KeyJoiner = (Array<any>) => string;

export type Generator = (string, string, Array<string>, Array<any>, Array<any>, number, number) => ChartDefinition;

const _defaultKeyJoiner = keys => keys.join('-');

const _defaultChartGenerator = (type, name, labels, values): ChartDefinition => ({ type, name, x: labels, y: values });

export const flattenLeafs = (leafs: Array<Leaf>, showSummary: boolean = false): Array<any> => {
  const filterCondition = (value) => {
    if (!value.source.endsWith('leaf')) {
      return false;
    }
    if (!showSummary) {
      return value.source !== 'row-leaf';
    }
    return true;
  };
  return flatten(leafs.map(l => l.values.filter(v => filterCondition(v)).map(v => [l.key, v])));
};

export const defaultSeriesExtraction = (keyJoiner: KeyJoiner = _defaultKeyJoiner) => {
  return (results: Rows): ExtractedSeries => {
    // $FlowFixMe: Somehow flow is unable to infer that the result consists only of Leafs.
    const leafs: Array<Leaf> = results.filter(row => (row.source === 'leaf'));
    const xLabels = leafs.map(({ key }) => key);
    const flatLeafs = flattenLeafs(leafs, true);
    const valuesBySeries = {};
    flatLeafs.forEach(([key, value]) => {
      const joinedKey = keyJoiner(value.key);
      const targetIdx = xLabels.findIndex(l => isEqual(l, key));
      if (value.value) {
        set(valuesBySeries, [joinedKey, targetIdx], value.value);
      }
    });

    return Object.keys(valuesBySeries).map(value => [
      value,
      xLabels,
      valuesBySeries[value],
      [],
    ]);
  };
};

export const generateChart = (chartType: string, generator: Generator = _defaultChartGenerator): ((ExtractedSeries) => Array<ChartDefinition>) => {
  return (results: ExtractedSeries) => {
    const allCharts: Array<[string, string, Array<string>, Array<any>, Array<any>]> = results.map(([value, x, values, z]) => [
      chartType,
      value,
      x.map(key => key.join('-')),
      values,
      z,
    ]);

    return allCharts.map((args, idx) => generator(...args, idx, allCharts.length));
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

const doNotSuffixTraceForSingleSeries = keys => (keys.length > 1 ? keys.slice(0, -1).join('-') : keys[0]);

export const chartData = (
  { rowPivots, columnPivots, series }: AggregationWidgetConfig,
  data: Rows,
  chartType: string,
  generator: Generator = _defaultChartGenerator,
  customSeriesExtraction?: (keyJoiner: KeyJoiner) => (results: Rows) => ExtractedSeries,
): Array<ChartDefinition> => {
  const extractSeries = customSeriesExtraction || defaultSeriesExtraction;
  return flow([
    transformKeys(rowPivots, columnPivots),
    extractSeries(series.length === 1 ? doNotSuffixTraceForSingleSeries : _defaultKeyJoiner),
    removeNulls(),
    generateChart(chartType, generator),
  ])(data);
};
