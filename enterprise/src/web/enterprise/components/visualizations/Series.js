import Immutable from 'immutable';

const _defaultSeriesGenerator = (type, name, labels, values) => ({ type, name, x: labels, y: values });
export const generateSeries = (config, results, chartType, generator = _defaultSeriesGenerator) => {
  const { series } = config;
  const rowPivots = config.rowPivots.map(({ field }) => field);
  const columnPivots = config.columnPivots.map(({ field }) => field);
  const x = results.map(v => rowPivots.map(p => v[p]).join('-'));

  const columnPivotCharts = columnPivots.map((columnPivot) => {
    const values = {};
    const columnPivotX = results.filter(r => r[columnPivot])
      .map(row => row[columnPivot].map(pivot => pivot[columnPivot]))
      .reduce((prev, cur) => prev.merge(cur), Immutable.OrderedSet()).toJS();
    results.forEach((row) => {
      const valueKey = rowPivots.map(p => row[p]).join('-');
      columnPivotX.forEach((columnPivotValue) => {
        series.forEach((seriesName) => {
          const pivotValueKey = `${columnPivotValue}-${seriesName}`;

          if (!values[pivotValueKey]) {
            values[pivotValueKey] = [];
          }

          const rowValue = (row[columnPivot].find(r => r[columnPivot] === columnPivotValue) || {})[seriesName];
          values[`${columnPivotValue}-${seriesName}`].push({ key: valueKey, value: rowValue });
        });
      });
    });

    return Object.keys(values).map(pivotValue => [
      chartType,
      pivotValue,
      values[pivotValue].map(({ key }) => key),
      values[pivotValue].map(({ value }) => value),
    ]);
  }).flatten();

  const seriesCharts = series.map(s => [chartType, s, x, results.map(v => v[s])]);

  const allCharts = [].concat(seriesCharts, columnPivotCharts);
  return allCharts.map((args, idx) => generator(...args, idx, allCharts.length));
};
