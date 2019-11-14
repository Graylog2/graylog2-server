// @flow strict
import uuid from 'uuid/v4';
import { parseSeries } from 'views/logic/aggregationbuilder/Series';
import AggregationWidget from '../../aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from '../../aggregationbuilder/AggregationWidgetConfig';

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

const generateConfig = (id: string, { rollup, rowPivots, columnPivots, series, sort }: AggregationWidgetConfig) => ({
  id,
  type: 'pivot',
  config: {
    id: 'vals',
    rollup,
    row_groups: rowPivots.map(formatPivot),
    column_groups: columnPivots.map(formatPivot),
    series: series.map(s => Object.assign({ id: s.effectiveName }, parseSeries(s.function))),
    sort: sort,
  },
});

export default ({ config }: AggregationWidget) => {
  const chartSearchTypeId = uuid();
  return config.visualization === 'numeric' && config.visualizationConfig && config.visualizationConfig.trend
    ? [generateConfig(chartSearchTypeId, config), { ...(generateConfig(uuid(), config)), timerange: { type: 'offset', source: 'search_type', id: chartSearchTypeId } }]
    : [generateConfig(chartSearchTypeId, config)];
};
