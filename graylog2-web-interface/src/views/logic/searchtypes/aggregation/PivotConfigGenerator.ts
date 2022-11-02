/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import { Set } from 'immutable';
import type { $Shape } from 'utility-types';

import { parseSeries } from 'views/logic/aggregationbuilder/Series';
import type { Definition } from 'views/logic/aggregationbuilder/Series';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type { TimeConfigType, TimeUnitConfig } from 'views/logic/aggregationbuilder/Pivot';
import type Pivot from 'views/logic/aggregationbuilder/Pivot';
import type NumberVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/NumberVisualizationConfig';
import type { TimeUnit } from 'views/Constants';
import generateId from 'logic/generateId';

import type SortConfig from '../../aggregationbuilder/SortConfig';

const mapTimeunit = (unit: TimeUnit) => {
  switch (unit) {
    case 'seconds':
      return 's';
    case 'minutes':
      return 'm';
    case 'hours':
      return 'h';
    case 'days':
      return 'd';
    case 'weeks':
      return 'w';
    case 'months':
      return 'M';
    default:
      throw new Error(`Invalid time unit: ${unit}`);
  }
};

type FormattedInterval = {
  timeunit: string,
  type: string,
}

type FormattedPivot = {
  type: string,
  field: string,
  interval: FormattedInterval,
};

const formatPivot = (pivot: Pivot): FormattedPivot => {
  const { type, field, config } = pivot;
  const newConfig = { ...config } as unknown;

  switch (type) {
    // eslint-disable-next-line no-case-declarations
    case 'time':
      if ((config as TimeConfigType).interval.type === 'timeunit') {
        const { interval } = config as TimeConfigType;

        const { unit, value } = (interval as TimeUnitConfig);

        (newConfig as { interval: FormattedInterval }).interval = {
          type: 'timeunit',
          timeunit: `${value}${mapTimeunit(unit)}`,
        };
      }

      break;
    default:
  }

  return {
    type,
    field,
    ...(newConfig as { interval: FormattedInterval }),
  } as FormattedPivot;
};

type FormattedSeries = $Shape<{
  id: string,
} & Definition>;

const generateConfig = (id: string, name: string, {
  rollupForBackendQuery,
  rowPivots,
  columnPivots,
  series,
  sort,
  rowLimit,
  columnLimit,
}: AggregationWidgetConfig) => ({
  id,
  name,
  type: 'pivot',
  config: {
    id: 'vals',
    rollup: rollupForBackendQuery,
    row_groups: rowPivots.map(formatPivot),
    column_groups: columnPivots.map(formatPivot),
    series: series.map<FormattedSeries>((s) => ({ id: s.effectiveName, ...parseSeries(s.function) })),
    sort: sort,
    row_limit: rowLimit,
    column_limit: columnLimit,
  },
});

export default ({ config }: { config: AggregationWidgetConfig }) => {
  const chartConfig = generateConfig(generateId(), 'chart', config);

  // eslint-disable-next-line @typescript-eslint/no-use-before-define
  const configBuilder = ConfigBuilder.create([chartConfig]);

  // TODO: This should go into a visualization config specific function
  if (config.visualization === 'numeric' && config.visualizationConfig && (config.visualizationConfig as NumberVisualizationConfig).trend) {
    const trendConfig = {
      ...(generateConfig(generateId(), 'trend', config)),
      timerange: { type: 'offset', source: 'search_type', id: chartConfig.id },
    };

    configBuilder.add(trendConfig);
  }

  if (config.eventAnnotation) {
    const eventAnnotationConfig = {
      id: generateId(),
      name: 'events',
      type: 'events',
    };

    configBuilder.add(eventAnnotationConfig);
  }

  return configBuilder.build();
};

type Config = {
  id: string,
  name: string,
  type: string,
  config?: {
    id: string,
    rollup: boolean,
    row_groups: Array<FormattedPivot>,
    column_groups: Array<FormattedPivot>,
    series: Array<FormattedSeries>,
    sort: Array<SortConfig>,
  },
  timerange?: {
    type: string,
    source: string,
    id: string,
  },
};

class ConfigBuilder {
  value: Set<Config>;

  constructor(values: Array<any>) {
    this.value = Set.of(...values);
  }

  add(val: Config) {
    this.value = this.value.add(val);

    return this;
  }

  build(): Array<Config> {
    return this.value.toArray();
  }

  static create(values = []) {
    return new ConfigBuilder(values);
  }
}
