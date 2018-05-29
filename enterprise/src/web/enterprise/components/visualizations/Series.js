import { flatten, isEqual, set } from 'lodash';

const seriesRegex = /^(\w+)\((\w*)\)$/;

export const parseSeries = (s) => {
  const [_, type, field] = seriesRegex.exec(s);
  const definition = {
    type,
  };
  if (field !== '') {
    definition.field = field;
  }
  return definition;
};

const _defaultSeriesGenerator = (type, name, labels, values) => ({ type, name, x: labels, y: values });

export const generateSeries = (results, chartType, generator = _defaultSeriesGenerator) => {
  const leafs = results.filter(row => (row.source === 'leaf'));

  const x = leafs.map(({ key }) => key);

  const y = flatten(leafs.map(l => l.values.filter(v => v.source.endsWith('leaf')).map(v => [l.key, v])));

  const valuesBySeries = {};
  y.forEach(([key, value]) => {
    const joinedKey = value.key.join('-');
    const targetIdx = x.findIndex(l => isEqual(l, key));
    set(valuesBySeries, [joinedKey, targetIdx], value.value);
  });

  const allCharts = Object.keys(valuesBySeries).map((value) => {
    return [
      chartType,
      value,
      x.map(key => key.join('-')),
      valuesBySeries[value],
    ];
  });

  return allCharts.map((args, idx) => generator(...args, idx, allCharts.length));
};
