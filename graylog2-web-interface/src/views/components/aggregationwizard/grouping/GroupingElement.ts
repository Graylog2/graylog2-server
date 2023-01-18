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
import { isEmpty } from 'lodash';

import type { AggregationWidgetConfigBuilder } from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type { TimeConfigType, ValuesConfigType } from 'views/logic/aggregationbuilder/Pivot';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import generateId from 'logic/generateId';
import parseNumber from 'views/components/aggregationwizard/grouping/parseNumber';
import { DEFAULT_LIMIT } from 'views/Constants';

import GroupingsConfiguration from './GroupingsConfiguration';

import type { AggregationElement } from '../AggregationElementType';
import type {
  BaseGrouping,
  DateGrouping,
  GroupByFormValues,
  GroupingDirection,
  ValuesGrouping,
  WidgetConfigFormValues, WidgetConfigValidationErrors,
} from '../WidgetConfigForm';

export type GroupByError = {
  fields?: string,
  interval?: string,
  limit?: string,
};

export const isValuesGrouping = (grouping: GroupByFormValues): grouping is ValuesGrouping => {
  return grouping.type === 'values';
};

export const isDateGrouping = (grouping: GroupByFormValues): grouping is DateGrouping => {
  return grouping.type === 'time';
};

const validateDateGrouping = (grouping: DateGrouping): GroupByError => {
  const groupByError = {} as GroupByError;

  if (!grouping.fields?.length) {
    groupByError.fields = 'Field is required.';
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
  const groupByError: GroupByError = {};

  if (!grouping.fields?.length) {
    groupByError.fields = 'Field is required.';
  }

  const parsedLimit = parseNumber(grouping.limit);

  if (parsedLimit === undefined) {
    groupByError.limit = 'Limit is required.';
  }

  if (grouping.limit <= 0) {
    groupByError.limit = 'Must be greater than 0.';
  }

  return groupByError;
};

const hasErrors = <T extends {}> (errors: Array<T>): boolean => {
  return errors.filter((error) => Object.keys(error).length > 0).length > 0;
};

const validateGrouping = (grouping: GroupByFormValues): GroupByError => {
  if ('interval' in grouping) {
    return validateDateGrouping(grouping);
  }

  return validateValuesGrouping(grouping);
};

const validateGroupings = (values: WidgetConfigFormValues): WidgetConfigValidationErrors => {
  const emptyErrors = {};

  if (!values.groupBy) {
    return emptyErrors;
  }

  const { groupings } = values.groupBy;
  const groupingErrors = groupings.map(validateGrouping);

  return hasErrors(groupingErrors) ? { groupBy: { groupings: groupingErrors } } : emptyErrors;
};

const addRandomId = <GroupingType extends BaseGrouping>(baseGrouping: Omit<GroupingType, 'id'>) => ({
  ...baseGrouping,
  id: generateId(),
});

const datePivotToGrouping = (pivot: Pivot, direction: GroupingDirection): DateGrouping => {
  const { fields, config } = pivot;

  const { interval } = config as TimeConfigType;

  return addRandomId<DateGrouping>({
    direction,
    fields,
    type: 'time',
    interval,
  });
};

const valuesPivotToGrouping = (pivot: Pivot, direction: GroupingDirection): ValuesGrouping => {
  const { fields, config } = pivot;
  const { limit } = config as ValuesConfigType;

  return addRandomId<ValuesGrouping>({
    direction,
    fields,
    type: 'values',
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

  return Pivot.create(grouping.fields, grouping.type, pivotConfig);
};

const groupByToConfig = (groupBy: WidgetConfigFormValues['groupBy'], config: AggregationWidgetConfigBuilder) => {
  const rowPivots = groupBy.groupings.filter((grouping) => grouping.direction === 'row').map(groupingToPivot);
  const columnPivots = groupBy.groupings.filter((grouping) => grouping.direction === 'column').map(groupingToPivot);
  const { columnRollup } = groupBy;

  return config
    .rowPivots(rowPivots)
    .columnPivots(columnPivots)
    .rollup(columnRollup);
};

export const createEmptyGrouping = () => addRandomId<ValuesGrouping>({
  direction: 'row',
  fields: [],
  type: 'values',
  limit: DEFAULT_LIMIT,
});

const GroupByElement: AggregationElement<'groupBy'> = {
  sectionTitle: 'Group By',
  title: 'Grouping',
  key: 'groupBy',
  order: 1,
  allowCreate: () => true,
  onCreate: (formValues: WidgetConfigFormValues) => ({
    ...formValues,
    groupBy: {
      columnRollup: formValues.groupBy ? formValues.groupBy.columnRollup : false,
      groupings: [
        ...(formValues.groupBy?.groupings ?? []),
        createEmptyGrouping(),
      ],
    },
  }),
  onRemove: ((index, formValues) => {
    const newFormValues = { ...formValues };
    const newGroupings = formValues.groupBy?.groupings.filter((_value, i) => (index !== i));

    return ({
      ...newFormValues,
      groupBy: {
        columnRollup: newFormValues.groupBy.columnRollup ?? false,
        groupings: newGroupings,
      },
    });
  }),
  fromConfig: (config: AggregationWidgetConfig) => {
    const groupings = pivotsToGrouping(config);

    if (isEmpty(groupings)) {
      return undefined;
    }

    return {
      groupBy: {
        columnRollup: config.rollup,
        groupings,
      },
    };
  },
  toConfig: (formValues: WidgetConfigFormValues, configBuilder: AggregationWidgetConfigBuilder) => groupByToConfig(formValues.groupBy, configBuilder),
  component: GroupingsConfiguration,
  validate: validateGroupings,
  isEmpty: (formValues: WidgetConfigFormValues['groupBy']) => (formValues?.groupings ?? []).length === 0,
};

export default GroupByElement;
