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
import { TimeUnits } from 'views/Constants';
import Pivot from 'views/logic/aggregationbuilder/Pivot';

import type { AggregationElement } from './AggregationElementType';

import type { GroupingDirection, DateGrouping, ValuesGrouping, GroupByFormValues, WidgetConfigFormValues } from '../WidgetConfigForm';
import GroupByConfiguration from '../elementConfiguration/GroupByConfiguration';

export type DatePivotConfig = {
  interval: {
    type: 'auto',
    scaling: number
  } | {
    type: 'timeunit',
    value: number,
    unit: keyof typeof TimeUnits,
  }
}

export type ValuesPivotConfig = {
  limit: number
}

type GroupByError = {
  field?: string,
  limit?: string,
  interval?: string,
};

const validateDateGrouping = (grouping: DateGrouping): GroupByError => {
  const groupByError = {} as GroupByError;

  if (!grouping.field?.field) {
    groupByError.field = 'Field is required.';
  }

  if (grouping.interval.type === 'auto') {
    if (!grouping.interval.scaling) {
      groupByError.interval = 'Scaling is required.';
    }

    if (grouping.interval.scaling && (grouping.interval.scaling <= 0 || grouping.interval.scaling >= 10)) {
      groupByError.interval = 'Must be greater than 0 and smaller or equals 10.';
    }
  }

  if (grouping.interval.type === 'timeunit') {
    if (!grouping.interval.value) {
      groupByError.interval = 'Value is required.';
    }

    if (grouping.interval.value && grouping.interval.value <= 0) {
      groupByError.interval = 'Must be greater than 0.';
    }
  }

  return groupByError;
};

const validateValuesGrouping = (grouping: ValuesGrouping): GroupByError => {
  const groupByError = {} as GroupByError;

  if (!grouping.field?.field) {
    groupByError.field = 'Field is required.';
  }

  if (!grouping.limit) {
    groupByError.limit = 'Limit is required.';
  }

  if (grouping.limit && grouping.limit <= 0) {
    groupByError.limit = 'Must be greater than 0.';
  }

  return groupByError;
};

const hasErrors = <T extends {}>(errors: Array<T>): boolean => {
  return errors.filter((error) => Object.keys(error).length > 0).length > 0;
};

const validateGrouping = (grouping: GroupByFormValues): GroupByError => {
  if ('interval' in grouping) {
    const dateGrouping = grouping as DateGrouping;

    return validateDateGrouping(dateGrouping);
  }

  const valuesGrouping = grouping as ValuesGrouping;

  return validateValuesGrouping(valuesGrouping);
};

const validateGroupBy = (values: WidgetConfigFormValues) => {
  const emptyErrors = {};

  if (!values.groupBy) {
    return emptyErrors;
  }

  const { groupings } = values.groupBy;
  const groupByErrors = groupings.map(validateGrouping);

  return hasErrors(groupByErrors) ? { groupBy: { groupings: groupByErrors } } : emptyErrors;
};

const datePivotToGrouping = (pivot: Pivot, direction: GroupingDirection): DateGrouping => {
  const { field, config } = pivot;

  const { interval } = config as DatePivotConfig;

  return ({
    direction,
    field: { field, type: 'time' },
    interval,
  });
};

const valuesPivotToGrouping = (pivot: Pivot, direction: GroupingDirection): ValuesGrouping => {
  const { field, config } = pivot;
  const { limit } = config as ValuesPivotConfig;

  return ({
    direction,
    field: { field, type: 'values' },
    limit,
  });
};

const pivotToGroupings = (pivot: Pivot, direction: GroupingDirection): GroupByFormValues => {
  if (pivot.type === 'time') {
    return datePivotToGrouping(pivot, direction);
  }

  if (pivot.type === 'values') {
    return valuesPivotToGrouping(pivot, direction);
  }

  throw new Error(`Widget has not supported pivot type "${pivot.type}"`);
};

const pivotsToGrouping = (config: AggregationWidgetConfig) => {
  const transformedRowPivots = config.rowPivots.map((pivot) => pivotToGroupings(pivot, 'row'));
  const transformedColumnPivots = config.columnPivots.map((pivot) => pivotToGroupings(pivot, 'column'));

  return [...transformedRowPivots, ...transformedColumnPivots];
};

const groupingToPivot = (grouping: GroupByFormValues) => {
  const pivotConfig = 'interval' in grouping ? { interval: grouping.interval } : { limit: grouping.limit };

  return new Pivot(grouping.field.field, grouping.field.type, pivotConfig);
};

const groupByToConfig = (groupBy: WidgetConfigFormValues['groupBy'], config: AggregationWidgetConfig) => {
  const rowPivots = groupBy.groupings.filter((grouping) => grouping.direction === 'row').map(groupingToPivot);
  const columnPivots = groupBy.groupings.filter((grouping) => grouping.direction === 'column').map(groupingToPivot);
  const { columnRollup } = groupBy;

  return config.toBuilder()
    .rowPivots(rowPivots)
    .columnPivots(columnPivots)
    .rollup(columnRollup)
    .build();
};

export const emptyGrouping: ValuesGrouping = {
  direction: 'row',
  field: {
    field: undefined,
    type: 'values',
  },
  limit: 15,
};

const GroupByElement: AggregationElement = {
  title: 'Group By',
  key: 'groupBy',
  order: 1,
  allowCreate: () => true,
  addEmptyElement: (formValues: WidgetConfigFormValues): WidgetConfigFormValues => ({
    ...formValues,
    groupBy: {
      columnRollup: 'columnRollup' in formValues.groupBy ? formValues.groupBy.columnRollup : true,
      groupings: [
        ...formValues.groupBy.groupings,
        emptyGrouping,
      ],
    },
  }),
  fromConfig: (config: AggregationWidgetConfig) => ({
    groupBy: {
      columnRollup: config.rollup,
      groupings: pivotsToGrouping(config),
    },
  }),
  toConfig: (formValues: WidgetConfigFormValues, config: AggregationWidgetConfig) => groupByToConfig(formValues.groupBy, config),
  component: GroupByConfiguration,
  validate: validateGroupBy,
};

export default GroupByElement;
