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
import uuid from 'uuid/v4';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { TIMESTAMP_FIELD, DEFAULT_MESSAGE_FIELDS } from 'views/Constants';

import pivotForField from './searchtypes/aggregation/PivotGenerator';
import AggregationWidget from './aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from './aggregationbuilder/AggregationWidgetConfig';
import MessageWidget from './widgets/MessagesWidget';
import MessageWidgetConfig from './widgets/MessagesWidgetConfig';
import Series from './aggregationbuilder/Series';
import FieldType from './fieldtypes/FieldType';
import type { Decorator } from './widgets/MessagesWidgetConfig';

const widgetsKey = 'enterpriseWidgets';

const _findWidgetDefinition = (type) => PluginStore.exports(widgetsKey)
  .find((widget) => widget.type.toLocaleUpperCase() === type.toLocaleUpperCase());

export function widgetDefinition(type: string) {
  const typeDefinition = _findWidgetDefinition(type);

  if (typeDefinition) {
    return typeDefinition;
  }

  const defaultType = _findWidgetDefinition('default');

  if (defaultType) {
    return defaultType;
  }

  throw new Error(`Neither a widget of type "${type}" nor a default widget are registered!`);
}

export const resultHistogram = (id: string = uuid()) => AggregationWidget.builder()
  .id(id)
  .config(
    AggregationWidgetConfig.builder()
      .columnPivots([])
      .rowPivots([
        pivotForField(TIMESTAMP_FIELD, new FieldType('date', [], [])),
      ])
      .series([
        Series.forFunction('count()'),
      ])
      .sort([])
      .visualization('bar')
      .rollup(true)
      .build(),
  )
  .build();

export const allMessagesTable = (id: string = uuid(), decorators: Array<Decorator>) => MessageWidget.builder()
  .id(id)
  .config(MessageWidgetConfig.builder()
    .fields(DEFAULT_MESSAGE_FIELDS)
    .showMessageRow(true)
    .decorators(decorators)
    .build())
  .build();
