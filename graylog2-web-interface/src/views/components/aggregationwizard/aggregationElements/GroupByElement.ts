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
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import type { DatePivotConfig, ValuesPivotConfig } from 'views/logic/aggregationbuilder/Pivot';

import type { AggregationElement } from './AggregationElementType';

import type { GroupingDirection, DateGrouping, ValuesGrouping, GroupByFormValues, WidgetConfigFormValues } from '../WidgetConfigForm';
import GroupByConfiguration from '../elementConfiguration/GroupByConfiguration';

const datePivotToGroupBy = (pivot: Pivot, direction: GroupingDirection): DateGrouping => {
  const { field, config } = pivot;
  const { interval } = config as DatePivotConfig;

  return ({
    field,
    direction,
    interval,
  });
};

const valuesPivotToGroupBy = (pivot: Pivot, direction: GroupingDirection): ValuesGrouping => {
  const { field, config } = pivot;
  const { limit } = config as ValuesPivotConfig;

  return ({
    field,
    direction,
    limit,
  });
};

const pivotToGroupBy = (pivot: Pivot, direction: GroupingDirection): GroupByFormValues => {
  if (pivot.type === 'time') {
    return datePivotToGroupBy(pivot, direction);
  }

  if (pivot.type === 'values') {
    return valuesPivotToGroupBy(pivot, direction);
  }

  throw new Error(`Widget has not supported pivot type "${pivot.type}"`);
};

const pivotsToGroupBy = (config: AggregationWidgetConfig) => {
  const transformedRowPivots = config.rowPivots.map((pivot) => pivotToGroupBy(pivot, 'row'));
  const transformedColumnPivots = config.columnPivots.map((pivot) => pivotToGroupBy(pivot, 'column'));

  return [...transformedRowPivots, ...transformedColumnPivots];
};

const groupByToPivot = (groupBy: GroupByFormValues) => {
  const pivotConfig = 'interval' in groupBy ? { interval: groupBy.interval } : { limit: groupBy.limit };

  return new Pivot(groupBy.field, 'interval' in groupBy ? 'time' : 'values', pivotConfig);
};

const groupByToConfigWithPivots = (groupByEntires: Array<GroupByFormValues>, config: AggregationWidgetConfig) => {
  const rowPivots = groupByEntires.filter((groupBy) => groupBy.direction === 'row').map(groupByToPivot);
  const columnPivots = groupByEntires.filter((groupBy) => groupBy.direction === 'column').map(groupByToPivot);

  return config.toBuilder().rowPivots(rowPivots).columnPivots(columnPivots).build();
};

const GroupByElement: AggregationElement = {
  title: 'Group By',
  key: 'groupBy',
  order: 1,
  allowCreate: () => true,
  createEmpty: (): GroupByFormValues => ({
    direction: 'row',
    field: undefined,
    limit: 15,
  }),
  fromConfig: (config: AggregationWidgetConfig) => ({
    groupBy: pivotsToGroupBy(config),
  }),
  toConfig: (formValues: WidgetConfigFormValues, config: AggregationWidgetConfig) => groupByToConfigWithPivots(formValues.groupBy, config),
  component: GroupByConfiguration,
};

export default GroupByElement;
