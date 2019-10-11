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
};

export type ChartData = [any, Array<Key>, Array<any>]
export type ExtractedSeries = Array<ChartData>;

export type KeyJoiner = (Array<any>) => string;

export type Generator = (string, string, Array<string>, Array<any>, number, number) => ChartDefinition;

const _defaultKeyJoiner = keys => keys.join('-');

export const extractSeries = (keyJoiner: KeyJoiner = _defaultKeyJoiner) => {
  return (results: Rows): ExtractedSeries => {
    // $FlowFixMe: Somehow flow is unable to infer that the result consists only of Leafs.
    const leafs: Array<Leaf> = results.filter(row => (row.source === 'leaf'));

    const x = leafs.map(({ key }) => key);

    const y = flatten(leafs.map(l => l.values.filter(v => v.source.endsWith('leaf')).map(v => [l.key, v])));

    const valuesBySeries = {};
    y.forEach(([key, value]) => {
      const joinedKey = keyJoiner(value.key);
      const targetIdx = x.findIndex(l => isEqual(l, key));
      if (value.value) {
        set(valuesBySeries, [joinedKey, targetIdx], value.value);
      }
    });

    return Object.keys(valuesBySeries).map(value => [
      value,
      x,
      valuesBySeries[value],
    ]);
  };
};

const _defaultChartGenerator = (type, name, labels, values): ChartDefinition => ({ type, name, x: labels, y: values });

export const generateChart = (chartType: string, generator: Generator = _defaultChartGenerator): ((ExtractedSeries) => Array<ChartDefinition>) => {
  return (results: ExtractedSeries) => {
    const allCharts: Array<[string, string, Array<string>, Array<any>]> = results.map(([value, x, values]) => [
      chartType,
      value,
      x.map(key => key.join('-')),
      values,
    ]);

    return allCharts.map((args, idx) => generator(...args, idx, allCharts.length));
  };
};

export const removeNulls = (): ((ExtractedSeries) => ExtractedSeries) => {
  return (results: ExtractedSeries) => results.map(([name, keys, values]) => {
    const nullIndices = Array.from(values).reduce((indices, value, index) => ((value === null || value === undefined) ? [...indices, index] : indices), []);
    const newKeys = keys.filter((_, idx) => !nullIndices.includes(idx));
    const newValues = values.filter((_, idx) => !nullIndices.includes(idx));
    return [name, newKeys, newValues];
  });
};

const doNotSuffixTraceForSingleSeries = keys => (keys.length > 1 ? keys.slice(0, -1).join('-') : keys[0]);

export const chartData = (
  { rowPivots, columnPivots, series }: AggregationWidgetConfig,
  data: Rows,
  chartType: string,
  generator: Generator = _defaultChartGenerator,
): Array<ChartDefinition> => flow([
  transformKeys(rowPivots, columnPivots),
  extractSeries(series.length === 1 ? doNotSuffixTraceForSingleSeries : undefined),
  removeNulls(),
  generateChart(chartType, generator),
])(data);
