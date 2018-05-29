import * as Series from 'enterprise/components/visualizations/Series';

const formatPivot = (pivot) => {
  const { type, field, config } = pivot;
  const newConfig = Object.assign({}, config);

  switch (type) {
    // eslint-disable-next-line no-case-declarations
    case 'time':
      const { unit, value } = newConfig.interval;
      newConfig.interval = `${value}${unit[0]}`;
      break;
    default:
  }

  return {
    type,
    field,
    ...newConfig,
  };
};

export default ({ columnPivots, rowPivots, series }) => [{
  type: 'pivot',
  config: {
    id: 'vals',
    rollup: true,
    row_groups: rowPivots.map(formatPivot),
    column_groups: columnPivots.map(formatPivot),
    series: series.map(Series.parseSeries),
  },
}];
