// @flow strict
import uuid from 'uuid/v4';
import { Set } from 'immutable';
import { parseSeries } from 'views/logic/aggregationbuilder/Series';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import type { TimeUnit } from '../../../Constants';

const mapTimeunit = (unit: TimeUnit) => {
  switch (unit) {
    case 'seconds': return 's';
    case 'minutes': return 'm';
    case 'hours': return 'h';
    case 'days': return 'd';
    case 'weeks': return 'w';
    case 'months': return 'M';
    default: throw new Error(`Invalid time unit: ${unit}`);
  }
};

type UnitValueInterval = { unit: TimeUnit, value: number };

const convertToDays = (interval: UnitValueInterval) => {
  const { unit, value } = interval;
  switch (unit) {
    case 'seconds':
    case 'minutes':
    case 'hours':
    case 'days': return interval;
    case 'weeks': return { unit: 'days', value: value * 7 };
    case 'months': return { unit: 'days', value: value * 30 };
    default: throw new Error(`Invalid time unit: ${unit}`);
  }
};

const adjustUnitsLongerThanDays = (interval: UnitValueInterval) => {
  const { unit, value } = interval;
  switch (unit) {
    case 'seconds':
    case 'minutes':
    case 'hours':
    case 'days': return interval;
    case 'weeks':
    case 'months': return value <= 1 ? interval : convertToDays(interval);
    default: throw new Error(`Invalid time unit: ${unit}`);
  }
};

const mapToTimeUnitInterval = (interval: UnitValueInterval) => {
  const { unit, value } = adjustUnitsLongerThanDays(interval);
  return { type: 'timeunit', timeunit: `${value}${mapTimeunit(unit)}` };
};

const formatPivot = (pivot: Pivot) => {
  const { type, field, config } = pivot;
  const newConfig = Object.assign({}, config);

  switch (type) {
    // eslint-disable-next-line no-case-declarations
    case 'time':
      if (newConfig.interval.type === 'timeunit') {
        /* $FlowFixMe: newConfig.interval has unit and value since it is from type timeunit */
        newConfig.interval = mapToTimeUnitInterval(newConfig.interval);
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

const generateConfig = (id: string, name: string, { rollup, rowPivots, columnPivots, series, sort }: AggregationWidgetConfig) => ({
  id,
  name,
  type: 'pivot',
  config: {
    id: 'vals',
    rollup,
    row_groups: rowPivots.map(formatPivot),
    column_groups: columnPivots.map(formatPivot),
    series: series.map(s => Object.assign({}, { id: s.effectiveName }, parseSeries(s.function))),
    sort: sort,
  },
});

export default ({ config }: { config: AggregationWidgetConfig }) => {
  const chartConfig = generateConfig(uuid(), 'chart', config);

  // eslint-disable-next-line no-use-before-define
  const configBuilder = ConfigBuilder.create([chartConfig]);

  // TODO: This should go into a visualization config specific function
  // $FlowFixMe: This is a NumberVisualizationConfig. We know so for config.visualization is 'numeric'.
  if (config.visualization === 'numeric' && config.visualizationConfig && config.visualizationConfig.trend) {
    const trendConfig = {
      ...(generateConfig(uuid(), 'trend', config)),
      timerange: { type: 'offset', source: 'search_type', id: chartConfig.id },
    };
    configBuilder.add(trendConfig);
  }

  if (config.eventAnnotation) {
    const eventAnnotationConfig = {
      id: uuid(),
      name: 'events',
      type: 'events',
    };
    configBuilder.add(eventAnnotationConfig);
  }

  return configBuilder.build();
};

class ConfigBuilder {
  value: Set;

  constructor(values: Array<any>) {
    this.value = Set.of(...values);
  }

  add(val) {
    this.value = this.value.add(val);
    return this;
  }

  build() {
    return this.value.toArray();
  }

  static create(values = []) {
    return new ConfigBuilder(values);
  }
}
