import { parseSeries } from 'enterprise/logic/aggregationbuilder/Series';

const formatPivot = (pivot) => {
  const { type, field, config } = pivot;
  const newConfig = Object.assign({}, config);

  switch (type) {
    // eslint-disable-next-line no-case-declarations
    case 'time':
      if (newConfig.interval.type === 'timeunit') {
        const { unit, value } = newConfig.interval;
        newConfig.interval = { type: 'timeunit', timeunit: `${value}${unit[0]}` };
      }
      break;
    default:
  }

  return {
    type,
    field,
    ...newConfig,
  };
};

export default ({ columnPivots, rowPivots, series, rollup, sort }) => [{
  type: 'pivot',
  config: {
    id: 'vals',
    rollup,
    row_groups: rowPivots.map(formatPivot),
    column_groups: columnPivots.map(formatPivot),
    series: series.map(s => Object.assign({ id: s.effectiveName }, parseSeries(s.function))),
    sort: sort,
  },
}];
