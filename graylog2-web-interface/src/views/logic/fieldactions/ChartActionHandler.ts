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
// @flow strict
import * as Immutable from 'immutable';

import Widget from 'views/logic/widgets/Widget';
import { WidgetActions } from 'views/stores/WidgetStore';
import pivotForField from 'views/logic/searchtypes/aggregation/PivotGenerator';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import Series, { isFunction } from 'views/logic/aggregationbuilder/Series';
import { FieldTypesStore } from 'views/stores/FieldTypesStore';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';

import type { FieldActionHandler } from './FieldActionHandler';
import duplicateCommonWidgetSettings from './DuplicateCommonWidgetSettings';

import FieldType from '../fieldtypes/FieldType';
import FieldTypeMapping from '../fieldtypes/FieldTypeMapping';

const TIMESTAMP_FIELD = 'timestamp';

const fieldTypeFor = (fieldName: string, queryId: string): FieldType => {
  const _fieldTypes = FieldTypesStore.getInitialState();

  if (!_fieldTypes) {
    return FieldType.Unknown;
  }

  const { queryFields, all } = _fieldTypes;

  const fieldTypes: FieldTypeMappingsList = (!queryFields || queryFields.get(queryId, Immutable.List()).isEmpty()) ? all : queryFields.get(queryId, Immutable.List());

  if (!fieldTypes) {
    return FieldType.Unknown;
  }

  const mapping: FieldTypeMapping = (fieldTypes as FieldTypeMappingsList)
    .find((m: FieldTypeMapping) => m.name === fieldName, null, new FieldTypeMapping(fieldName, FieldType.Unknown));

  return mapping.type;
};

const ChartActionHandler: FieldActionHandler = ({ queryId, field, contexts: { widget: origWidget = Widget.empty() } }) => {
  const series = isFunction(field) ? Series.forFunction(field) : Series.forFunction(`avg(${field})`);
  const config = AggregationWidgetConfig.builder()
    .rowPivots([pivotForField(TIMESTAMP_FIELD, fieldTypeFor(TIMESTAMP_FIELD, queryId))])
    .series([series])
    .visualization('line')
    .rollup(true)
    .build();
  const widgetBuilder = AggregationWidget.builder()
    .newId()
    .config(config);

  const widget = duplicateCommonWidgetSettings(widgetBuilder, origWidget).build();

  return WidgetActions.create(widget);
};

export default ChartActionHandler;
