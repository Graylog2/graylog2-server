// @flow strict
import { flatten, isEqual, set } from 'lodash';

import type { Key, Leaf, Rows } from 'enterprise/logic/searchtypes/pivot/PivotHandler';

export const seriesRegex = /^(\w+)\((\w*)\)$/;

type Definition = {
  type: string,
  field?: string,
};

export const parseSeries = (s: string) => {
  const result = seriesRegex.exec(s);
  if (!result) {
    return null;
  }
  // eslint-disable-next-line no-unused-vars
  const [_, type, field] = result;
  const definition: Definition = {
    type,
  };
  if (field !== '') {
    definition.field = field;
  }
  return definition;
};

export const isFunction = (s: string) => seriesRegex.test(s);

type ChartDefinition = {
  type: string,
  name: string,
  x?: Array<string>,
  y?: Array<any>,
};

const _defaultSeriesGenerator = (type, name, labels, values): ChartDefinition => ({ type, name, x: labels, y: values });

type Series = [any, Array<Key>, Array<any>]
type ExtractedSeries = Array<Series>;

export const extractSeries = (results: Rows): ExtractedSeries => {
  // $FlowFixMe: Somehow flow is unable to infer that the result consists only of Leafs.
  const leafs: Array<Leaf> = results.filter(row => (row.source === 'leaf'));

  const x = leafs.map(({ key }) => key);

  const y = flatten(leafs.map(l => l.values.filter(v => v.source.endsWith('leaf')).map(v => [l.key, v])));

  const valuesBySeries = {};
  y.forEach(([key, value]) => {
    const joinedKey = value.key.join('-');
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

export type Generator = (string, string, Array<string>, Array<any>, number, number) => ChartDefinition;

export const generateSeries = (results: Rows, chartType: string, generator: Generator = _defaultSeriesGenerator): Array<ChartDefinition> => {
  const allCharts: Array<[string, string, Array<string>, Array<any>]> = extractSeries(results).map(([value, x, values]) => [
    chartType,
    value,
    x.map(key => key.join('-')),
    values,
  ]);

  return allCharts.map((args, idx) => generator(...args, idx, allCharts.length));
};
