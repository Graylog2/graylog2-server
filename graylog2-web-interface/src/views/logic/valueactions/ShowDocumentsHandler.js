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
import { DEFAULT_MESSAGE_FIELDS } from 'views/Constants';
import { WidgetActions } from 'views/stores/WidgetStore';
import { escape, addToQuery } from 'views/logic/queries/QueryHelper';
import TitleTypes from 'views/stores/TitleTypes';

import type { ValueActionHandler, ValuePath } from './ValueActionHandler';

import MessagesWidget from '../widgets/MessagesWidget';
import MessagesWidgetConfig from '../widgets/MessagesWidgetConfig';
import View from '../views/View';
import Widget from '../widgets/Widget';
import { createElasticsearchQueryString } from '../queries/Query';
import { TitlesActions } from '../../stores/TitlesStore';
import duplicateCommonWidgetSettings from '../fieldactions/DuplicateCommonWidgetSettings';

type Contexts = {
  valuePath: ValuePath,
  view: View,
  widget: Widget,
};

type Arguments = {
  contexts: Contexts;
};

const extractFieldsFromValuePath = (valuePath: ValuePath): Array<string> => {
  return valuePath.map((item) => Object.entries(item)
    // $FlowFixMe: We know that values are strings
    .map(([key, value]: [string, string]) => (
      key === '_exists_' ? value : key)))
    .reduce((prev, cur) => [...prev, ...cur], [])
    .reduce((prev, cur) => (prev.includes(cur) ? prev : [...prev, cur]), []);
};

const ShowDocumentsHandler: ValueActionHandler = ({ contexts: { valuePath, widget } }: Arguments) => {
  // $FlowFixMe: Needs to be typed properly, right now contains an arbitrary set of keys/values.
  const mergedObject = valuePath.reduce((elem, acc) => ({
    ...acc,
    ...elem,
  }), {});
  const widgetQuery = widget && widget.query ? widget.query.query_string : '';
  const valuePathQuery = Object.entries(mergedObject)
    .map(([k, v]) => `${k}:${escape(String(v))}`)
    .reduce((prev: string, next: string) => addToQuery(prev, next), '');
  const query = addToQuery(widgetQuery, valuePathQuery);
  const valuePathFields = extractFieldsFromValuePath(valuePath);
  const messageListFields = new Set([...DEFAULT_MESSAGE_FIELDS, ...valuePathFields]);
  const newWidget = duplicateCommonWidgetSettings(MessagesWidget.builder(), widget)
    .query(createElasticsearchQueryString(query))
    .newId()
    .config(new MessagesWidgetConfig.builder()
      .fields([...messageListFields])
      .showMessageRow(true).build())
    .build();

  const title = `Messages for ${valuePathQuery}`;

  return WidgetActions.create(newWidget).then(() => TitlesActions.set(TitleTypes.Widget, newWidget.id, title));
};

ShowDocumentsHandler.isEnabled = ({ contexts: { valuePath, widget } }) => (valuePath !== undefined && widget !== undefined);

export default ShowDocumentsHandler;
