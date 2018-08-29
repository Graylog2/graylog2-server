import { flatten, isEqual, set } from 'lodash';

export const seriesRegex = /^(\w+)\((\w*)\)$/;

export const parseSeries = (s) => {
  // eslint-disable-next-line no-unused-vars
  const [_, type, field] = seriesRegex.exec(s);
  const definition = {
    type,
  };
  if (field !== '') {
    definition.field = field;
  }
  return definition;
};

export const isFunction = s => seriesRegex.test(s);

const _defaultSeriesGenerator = (type, name, labels, values) => ({ type, name, x: labels, y: values });

export const extractSeries = (results) => {
  const leafs = results.filter(row => (row.source === 'leaf'));

  const x = leafs.map(({ key }) => key);

  const y = flatten(leafs.map(l => l.values.filter(v => v.source.endsWith('leaf')).map(v => [l.key, v])));

  const valuesBySeries = {};
  y.forEach(([key, value]) => {
    const joinedKey = value.key.join('-');
    const targetIdx = x.findIndex(l => isEqual(l, key));
    set(valuesBySeries, [joinedKey, targetIdx], value.value);
  });

  return Object.keys(valuesBySeries).map(value => [
    value,
    x,
    valuesBySeries[value],
  ]);
};

export const generateSeries = (results, chartType, generator = _defaultSeriesGenerator) => {
  const allCharts = extractSeries(results).map(([value, x, values]) => [
    chartType,
    value,
    x.map(key => key.join('-')),
    values,
  ]);

  return allCharts.map((args, idx) => generator(...args, idx, allCharts.length));
};
