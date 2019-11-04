// @flow strict
import { flatten, flow, isEqual, set } from 'lodash';

import type { Key, Leaf, Rows, Value } from 'views/logic/searchtypes/pivot/PivotHandler';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';
import transformKeys from './TransformKeys';


export type ChartDefinition = {
  type: string,
  name: string,
  x?: Array<string>,
  y?: Array<any>,
  z?: Array<Array<any>>,
};

export type ChartData = [any, Array<Key>, Array<any>, Array<Array<any>>]
export type ExtractedSeries = Array<ChartData>;

export type KeyJoiner = (Array<any>) => string;

export type Generator = (string, string, Array<string>, Array<any>, Array<Array<any>>, number, number, Array<Pivot>, Array<Pivot>, Array<Series>) => ChartDefinition;

const _defaultKeyJoiner = keys => keys.join('-');

const _defaultChartGenerator = (type, name, labels, values): ChartDefinition => ({ type, name, x: labels, y: values });

const _flattenLeafs = (leafs: Array<Leaf>, matcher: Value => boolean = ({ source }) => source.endsWith('leaf')) => {
  return flatten(leafs.map(l => l.values.filter(value => matcher(value)).map(v => [l.key, v])));
};

export const formatSeries = ({ valuesBySeries, xLabels }: {valuesBySeries: Object, xLabels: Array<any>}): ExtractedSeries => {
  return Object.keys(valuesBySeries).map(value => [
    value,
    xLabels,
    valuesBySeries[value],
    [],
  ]);
};

export const extractSeries = (keyJoiner: KeyJoiner = _defaultKeyJoiner, leafValueMatcher?: Value => boolean) => {
  return (results: Rows) => {
    // $FlowFixMe: Somehow flow is unable to infer that the result consists only of Leafs.
    const leafs: Array<Leaf> = results.filter(row => (row.source === 'leaf'));
    const xLabels: Array<any> = leafs.map(({ key }) => key);
    const flatLeafs = _flattenLeafs(leafs, leafValueMatcher);
    const valuesBySeries = {};

    flatLeafs.forEach(([key, value]) => {
      const joinedKey = keyJoiner(value.key);
      const targetIdx = xLabels.findIndex(l => isEqual(l, key));
      if (value.value) {
        set(valuesBySeries, [joinedKey, targetIdx], value.value);
      }
    });
    return { valuesBySeries, xLabels };
  };
};

export const generateChart = (chartType: string, generator: Generator = _defaultChartGenerator, rowPivots: Array<Pivot>, columnPivots: Array<Pivot>, series: Array<Series>): ((ExtractedSeries) => Array<ChartDefinition>) => {
  return (results: ExtractedSeries) => {
    const allCharts: Array<[string, string, Array<string>, Array<any>, Array<Array<any>>]> = results.map(([value, x, values, z]) => [
      chartType,
      value,
      x.map(key => key.join('-')),
      values,
      z,
    ]);

    return allCharts.map((args, idx) => generator(...args, idx, allCharts.length, rowPivots, columnPivots, series));
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
  customSeriesFormatter?: ({valuesBySeries: Object, xLabels: Array<any>}) => ExtractedSeries = formatSeries,
  leafValueMatcher?: Value => boolean,
): Array<ChartDefinition> => {
  return flow([
    transformKeys(rowPivots, columnPivots),
    extractSeries(series.length === 1 ? doNotSuffixTraceForSingleSeries : undefined, leafValueMatcher),
    customSeriesFormatter,
    removeNulls(),
    generateChart(chartType, generator, rowPivots, columnPivots, series),
  ])(data);
};
