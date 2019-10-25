// @flow strict
import { flatten, flow, isEqual, set, includes } from 'lodash';

import type { Key, Leaf, Rows } from 'views/logic/searchtypes/pivot/PivotHandler';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import transformKeys from './TransformKeys';

export type ChartDefinition = {
  type: string,
  name: string,
  x?: Array<string>,
  y?: Array<any>,
};

export type ChartData = [any, Array<Key>, Array<any>, Array<any>]
export type ExtractedSeries = Array<ChartData>;

export type KeyJoiner = (Array<any>) => string;

export type Generator = (string, string, Array<string>, Array<any>, Array<any>, number, number) => ChartDefinition;


const XYZ_CHARTS = ['heatmap'];

const _defaultKeyJoiner = keys => keys.join('-');

const _defaultChartGenerator = (type, name, labels, values): ChartDefinition => ({ type, name, x: labels, y: values });

const _flattenLeafs = (leafs: Array<Leaf>, extractRowLeafs: boolean = false) => {
  const filterCondition = (value) => {
    if (!value.source.endsWith('leaf')) {
      return false;
    }
    if (extractRowLeafs) {
      return value.source !== 'row-leaf';
    }
    return true;
  };
  return flatten(leafs.map(l => l.values.filter(v => filterCondition(v)).map(v => [l.key, v])));
};

const _extractSeriesXY = (valuesBySeries, xLabels): ExtractedSeries => {
  return Object.keys(valuesBySeries).map(value => [
    value,
    xLabels,
    valuesBySeries[value],
    [],
  ]);
};

const _extractSeriesXYZ = (valuesBySeries, xLabels): ExtractedSeries => {
  const y = Object.keys(valuesBySeries);
  const z = Object.values(valuesBySeries);
  return [[
    'Hidden label',
    xLabels,
    y,
    z,
  ]];
};

export const extractSeries = (chartType: string, keyJoiner: KeyJoiner = _defaultKeyJoiner) => {
  return (results: Rows) => {
    const isXYZChart = includes(XYZ_CHARTS, chartType);
    // $FlowFixMe: Somehow flow is unable to infer that the result consists only of Leafs.
    const leafs: Array<Leaf> = results.filter(row => (row.source === 'leaf'));
    const xLabels = leafs.map(({ key }) => key);
    const flatLeafs = _flattenLeafs(leafs, isXYZChart);
    const valuesBySeries = {};

    flatLeafs.forEach(([key, value]) => {
      const joinedKey = keyJoiner(value.key);
      const targetIdx = xLabels.findIndex(l => isEqual(l, key));
      if (value.value) {
        set(valuesBySeries, [joinedKey, targetIdx], value.value || 0);
      }
    });

    if (isXYZChart) {
      return _extractSeriesXYZ(valuesBySeries, xLabels);
    }
    return _extractSeriesXY(valuesBySeries, xLabels);
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
): Array<ChartDefinition> => flow([
  transformKeys(rowPivots, columnPivots),
  extractSeries(chartType, series.length === 1 ? doNotSuffixTraceForSingleSeries : undefined),
  removeNulls(),
  generateChart(chartType, generator),
])(data);
