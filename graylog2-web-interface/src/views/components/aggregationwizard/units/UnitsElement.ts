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
import mapValues from 'lodash/mapValues';

import type { AggregationWidgetConfigBuilder } from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import UnitsConfig from 'views/logic/aggregationbuilder/UnitsConfig';
import FieldUnit from 'views/logic/aggregationbuilder/FieldUnit';

import type { AggregationElement } from '../AggregationElementType';
import type { WidgetConfigFormValues } from '../WidgetConfigForm';

const fromConfig = (config: AggregationWidgetConfig) => ({
  units: config.units.toFormValues(),
});

const toConfig = (formValues: WidgetConfigFormValues, configBuilder: AggregationWidgetConfigBuilder) => configBuilder
  .units(new UnitsConfig(mapValues(formValues.units, (unit) => new FieldUnit(unit.unitType, unit.abbrev))));

const validate = () => ({});

const UnitsElement: AggregationElement<'units'> = {
  title: 'Units',
  key: 'units',
  order: 5,
  allowCreate: () => false,
  component: () => null,
  fromConfig,
  toConfig,
  validate,
  isEmpty: () => false,
};

export default UnitsElement;
