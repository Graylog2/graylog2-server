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

import GroupByConfiguration from '../elementConfiguration/GroupByConfiguration';

type Direction = 'row' | 'column';

type DateGroupBy = {
  direction: Direction,
  field: string,
  type: 'time',
  interval: {
    type: 'auto',
    scaling: number,
  } | {
    type: 'timeunit',
    value: number,
    unit: string,
  }

}

type ValuesGroupBy = {
  direction: Direction,
  field: string,
  type: 'values',
  limit: number,
}

type GroupByEntry = DateGroupBy | ValuesGroupBy;

const datePivotToGroupBy = (pivot: Pivot, direction: Direction): DateGroupBy => {
  const { field, config } = pivot;
  const { interval } = config as DatePivotConfig;

  return ({
    type: 'time',
    field,
    direction,
    interval,
  });
};

const valuesPivotToGroupBy = (pivot: Pivot, direction: Direction): ValuesGroupBy => {
  const { field, config } = pivot;
  const { limit } = config as ValuesPivotConfig;

  return ({
    type: 'values',
    field,
    direction,
    limit,
  });
};

const pivotToGroupBy = (pivot: Pivot, direction: Direction): GroupByEntry => {
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

const GroupByElement: AggregationElement = {
  title: 'Group By',
  key: 'groupBy',
  order: 1,
  allowCreate: () => true,
  fromConfig: (config: AggregationWidgetConfig) => ({
    groupBy: pivotsToGroupBy(config),
  }),
  component: GroupByConfiguration,
};

export default GroupByElement;
