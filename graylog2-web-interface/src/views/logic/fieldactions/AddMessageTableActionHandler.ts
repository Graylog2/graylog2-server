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
import { DEFAULT_MESSAGE_FIELDS } from 'views/Constants';
import type { ViewsDispatch } from 'views/stores/useViewsDispatch';
import { addWidget } from 'views/logic/slices/widgetActions';

import MessagesWidget from '../widgets/MessagesWidget';
import MessagesWidgetConfig from '../widgets/MessagesWidgetConfig';

export const CreateMessagesWidget = () =>
  MessagesWidget.builder()
    .newId()
    .config(
      MessagesWidgetConfig.builder().fields(DEFAULT_MESSAGE_FIELDS).showMessageRow(true).showSummary(true).build(),
    )
    .build();

export default () => (dispatch: ViewsDispatch) => dispatch(addWidget(CreateMessagesWidget()));
