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
import AggregationWidgetConfig, { AggregationWidgetConfigBuilder } from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Direction from 'views/logic/aggregationbuilder/Direction';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';

import type { AggregationElement } from './AggregationElementType';

import SortConfiguration from '../elementConfigurationSections/SortConfiguration';
import { WidgetConfigFormValues } from '../WidgetConfigForm';

type SortError = {
  field?: string,
  direction?: string,
}

const hasErrors = <T extends {}>(errors: Array<T>): boolean => {
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

    return sortError;
  });

  return hasErrors(sortErrors) ? { sort: sortErrors } : {};
};

const configTypeToFormValueType = (type: 'pivot' | 'series') => {
  switch (type) {
    case 'pivot': return 'groupBy';
    case 'series': return 'metric';
    default: throw new Error(`Invalid sort type: ${type}`);
  }
};

const formValueTypeToConfigType = (type: 'groupBy' | 'metric') => {
  switch (type) {
    case 'groupBy': return 'pivot';
    case 'metric': return 'series';
    default: throw new Error(`Invalid sort type: ${type}`);
  }
};

const SortElement: AggregationElement = {
  title: 'Sort',
  key: 'sort',
  order: 3,
  allowAddEmptyElement: () => true,
  configurationSectionComponent: SortConfiguration,
  fromConfig: (config: AggregationWidgetConfig) => ({
    sort: config.sort.map((s) => ({
      type: configTypeToFormValueType(s.type),
      field: s.field,
      direction: s.direction?.direction,
    })),
  }),
  toConfig: (formValues: WidgetConfigFormValues, configBuilder: AggregationWidgetConfigBuilder) => configBuilder
    .sort(formValues.sort.map((sort) => new SortConfig(formValueTypeToConfigType(sort.type), sort.field, Direction.fromString(sort.direction)))),
  validate: validateSorts,
};

export default SortElement;
