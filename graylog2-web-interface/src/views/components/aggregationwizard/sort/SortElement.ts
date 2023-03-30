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
import type { AggregationWidgetConfigBuilder } from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Direction from 'views/logic/aggregationbuilder/Direction';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import generateId from 'logic/generateId';

import SortsConfiguration from './SortsConfiguration';

import type { AggregationElement } from '../AggregationElementType';
import type { WidgetConfigFormValues } from '../WidgetConfigForm';

type SortError = {
  field?: string,
  direction?: string,
}

const hasErrors = <T extends {}> (errors: Array<T>): boolean => {
  return errors.filter((error) => Object.keys(error).length > 0).length > 0;
};

const validateSorts = (values: WidgetConfigFormValues) => {
  const errors = {};

  if (!values.sort) {
    return errors;
  }

  const sortErrors = values.sort.map((sort) => {
    const sortError: SortError = {};

    if (!sort.field || sort.field === '') {
      sortError.field = 'Field is required.';
    }

    if (!sort.direction) {
      sortError.direction = 'Direction is required.';
    }

    const timeRowExists = !!values.groupBy?.groupings.find((g) => g.direction === 'row' && g.type === 'time');
    const nonDataTableVisExists = values.visualization && values.visualization.type !== 'table';

    if (timeRowExists && nonDataTableVisExists) {
      sortError.field = 'Sort on non data table with time based row grouping does not work.';
    }

    return sortError;
  });

  return hasErrors(sortErrors) ? { sort: sortErrors } : {};
};

const addRandomId = (baseSort = {}) => ({
  ...baseSort,
  id: generateId(),
});

const configTypeToFormValueType = (type: 'pivot' | 'series') => {
  switch (type) {
    case 'pivot':
      return 'groupBy';
    case 'series':
      return 'metric';
    default:
      throw new Error(`Invalid sort type: ${type}`);
  }
};

const formValueTypeToConfigType = (type: 'groupBy' | 'metric') => {
  switch (type) {
    case 'groupBy':
      return 'pivot';
    case 'metric':
      return 'series';
    default:
      throw new Error(`Invalid sort type: ${type}`);
  }
};

const SortElement: AggregationElement<'sort'> = {
  title: 'Sort',
  key: 'sort',
  order: 3,
  allowCreate: () => true,
  onCreate: (formValues) => ({
    ...formValues,
    sort: [
      ...formValues.sort,
      addRandomId({}),
    ],
  }),
  component: SortsConfiguration,
  fromConfig: (config: AggregationWidgetConfig) => ({
    sort: config.sort.map((s) => addRandomId({
      type: configTypeToFormValueType(s.type),
      field: s.field,
      direction: s.direction?.direction,
    })),
  }),
  toConfig: (formValues: WidgetConfigFormValues, configBuilder: AggregationWidgetConfigBuilder) => configBuilder
    .sort(formValues.sort.map((sort) => new SortConfig(formValueTypeToConfigType(sort.type), sort.field, Direction.fromString(sort.direction)))),
  onRemove: ((index, formValues) => ({
    ...formValues,
    sort: formValues.sort.filter((_value, i) => index !== i),
  })),
  validate: validateSorts,
  isEmpty: (formValues: WidgetConfigFormValues['sort']) => (formValues ?? []).length === 0,
};

export default SortElement;
