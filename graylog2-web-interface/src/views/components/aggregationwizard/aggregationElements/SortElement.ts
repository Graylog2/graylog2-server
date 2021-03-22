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

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Direction from 'views/logic/aggregationbuilder/Direction';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';

import type { AggregationElement } from './AggregationElementType';

import SortConfiguration from '../elementConfiguration/SortConfiguration';
import { WidgetConfigFormValues } from '../WidgetConfigForm';

const SortElement: AggregationElement = {
  title: 'Sort',
  key: 'sort',
  order: 3,
  allowCreate: (formValues: WidgetConfigFormValues) => isEmpty(formValues.sort),
  component: SortConfiguration,
  fromConfig: (config: AggregationWidgetConfig) => ({
    sort: config.sort.map((s) => ({
      field: s.field,
      direction: s.direction?.direction,
    })),
  }),
  toConfig: (formValues: WidgetConfigFormValues, config: AggregationWidgetConfig) => config.toBuilder()
    .sort(formValues.sort.map((sort) => new SortConfig('foo', sort.field, Direction.fromString(sort.direction))))
    .build(),
};

export default SortElement;
