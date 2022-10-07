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
import type { TimeConfigType } from 'views/logic/aggregationbuilder/Pivot';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import generateId from 'logic/generateId';
import parseNumber from 'views/components/aggregationwizard/grouping/parseNumber';

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

type GroupByError = {
  field?: string,
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
  const groupByError: GroupByError = {};

  if (!grouping.field?.field) {
    groupByError.field = 'Field is required.';
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

const DEFAULT_PIVOT_LIMIT = '15';

const validateGroupings = (values: WidgetConfigFormValues): WidgetConfigValidationErrors => {
  const emptyErrors = {};

  if (!values.groupBy) {
    return emptyErrors;
  }

  const errors = {};

  const groupByErrors: WidgetConfigValidationErrors['groupBy'] = {};
  const hasValuesRowPivots = values.groupBy?.groupings?.filter((grouping) => (grouping.direction === 'row' && grouping.field?.type === 'values')).length > 0;
  const hasValuesColumnPivots = values.groupBy?.groupings?.filter((grouping) => (grouping.direction === 'column' && grouping.field?.type === 'values')).length > 0;

  if (hasValuesRowPivots) {
    const parsedLimit = parseNumber(values.groupBy.rowLimit);

    if (parsedLimit === undefined) {
      groupByErrors.rowLimit = 'Row limit is required.';
    } else if (parsedLimit <= 0) {
      groupByErrors.rowLimit = 'Must be greater than 0.';
    }
  }

  if (hasValuesColumnPivots) {
    const parsedLimit = parseNumber(values.groupBy.columnLimit);

    if (parsedLimit === undefined) {
      groupByErrors.columnLimit = 'Column limit is required.';
    } else if (parsedLimit <= 0) {
      groupByErrors.columnLimit = 'Must be greater than 0.';
    }
  }

  const { groupings } = values.groupBy;
  const groupingErrors = groupings.map(validateGrouping);

  console.log('Returning errors: ', errors);

  return (hasErrors(groupingErrors) || Object.keys(groupByErrors).length > 0) ? { ...errors, groupBy: { ...groupByErrors, groupings: groupingErrors } } : emptyErrors;
};

const addRandomId = <GroupingType extends BaseGrouping>(baseGrouping: Omit<GroupingType, 'id'>) => ({
  ...baseGrouping,
  id: generateId(),
});

const datePivotToGrouping = (pivot: Pivot, direction: GroupingDirection): DateGrouping => {
  const { field, config } = pivot;

  const { interval } = config as TimeConfigType;

  return addRandomId<DateGrouping>({
    direction,
    field: { field, type: 'time' },
    interval,
  });
};

const valuesPivotToGrouping = (pivot: Pivot, direction: GroupingDirection): ValuesGrouping => {
  const { field } = pivot;

  return addRandomId<ValuesGrouping>({
    direction,
    field: { field, type: 'values' },
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
  const pivotConfig = 'interval' in grouping ? { interval: grouping.interval } : {};

  return new Pivot(grouping.field.field, grouping.field.type, pivotConfig);
};

const groupByToConfig = (groupBy: WidgetConfigFormValues['groupBy'], config: AggregationWidgetConfigBuilder) => {
  const rowPivots = groupBy.groupings.filter((grouping) => grouping.direction === 'row').map(groupingToPivot);
  const columnPivots = groupBy.groupings.filter((grouping) => grouping.direction === 'column').map(groupingToPivot);
  const { columnRollup } = groupBy;
  const rowLimit = rowPivots.length > 0 ? parseNumber(groupBy.rowLimit) : undefined;
  const columnLimit = columnPivots.length > 0 ? parseNumber(groupBy.columnLimit) : undefined;

  return config
    .rowPivots(rowPivots)
    .columnPivots(columnPivots)
    .rollup(columnRollup)
    .rowLimit(rowLimit)
    .columnLimit(columnLimit);
};

export const createEmptyGrouping: () => Partial<ValuesGrouping> = () => addRandomId<ValuesGrouping>({
  direction: 'row',
  field: {
    field: undefined,
    type: 'values',
  },
});

const GroupByElement: AggregationElement = {
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
      rowLimit: formValues.groupBy?.rowLimit ?? DEFAULT_PIVOT_LIMIT,
      columnLimit: formValues.groupBy?.columnLimit ?? DEFAULT_PIVOT_LIMIT,
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
        rowLimit: newFormValues.groupBy.rowLimit,
        columnLimit: newFormValues.groupBy.columnLimit,
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
        rowLimit: config.rowLimit ?? DEFAULT_PIVOT_LIMIT,
        columnLimit: config.columnLimit ?? DEFAULT_PIVOT_LIMIT,
      },
    };
  },
  toConfig: (formValues: WidgetConfigFormValues, configBuilder: AggregationWidgetConfigBuilder) => groupByToConfig(formValues.groupBy, configBuilder),
  component: GroupingsConfiguration,
  validate: validateGroupings,
};

export default GroupByElement;
