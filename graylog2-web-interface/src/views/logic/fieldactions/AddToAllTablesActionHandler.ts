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
import { WidgetStore, WidgetActions } from 'views/stores/WidgetStore';
import type { FieldActionHandler } from 'views/logic/fieldactions/FieldActionHandler';

const AddToAllTablesActionHandler: FieldActionHandler = ({ field }) => {
  const widgets = WidgetStore.getInitialState();
  const newWidgets = widgets.map((widget) => {
    if (widget.type.toUpperCase() === 'MESSAGES') {
      const newFields = [].concat(widget.config.fields, [field]);
      const newConfig = widget.config.toBuilder()
        .fields(newFields)
        .build();

      return widget.toBuilder().config(newConfig).build();
    }

    return widget;
  });

  return WidgetActions.updateWidgets(newWidgets);
};

export default AddToAllTablesActionHandler;
