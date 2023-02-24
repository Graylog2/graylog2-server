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
import type { ActionHandlerArguments } from 'views/components/actions/ActionHandler';
import type { AppDispatch } from 'stores/useAppDispatch';
import type { GetState } from 'views/types';
import { selectWidgets } from 'views/logic/slices/viewSelectors';
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import { updateWidgets } from 'views/logic/slices/widgetActions';

const RemoveFromAllTablesActionHandler = ({ field }: ActionHandlerArguments<{}>) => (dispatch: AppDispatch, getState: GetState) => {
  const widgets = selectWidgets(getState());
  const newWidgets = widgets.map((widget) => {
    if (widget.type.toUpperCase() === MessagesWidget.type.toUpperCase()) {
      const newFields = widget.config.fields.filter((f) => (f !== field));
      const newConfig = widget.config.toBuilder()
        .fields(newFields)
        .build();

      return widget.toBuilder().config(newConfig).build();
    }

    return widget;
  }).toList();

  return dispatch(updateWidgets(newWidgets));
};

export default RemoveFromAllTablesActionHandler;
