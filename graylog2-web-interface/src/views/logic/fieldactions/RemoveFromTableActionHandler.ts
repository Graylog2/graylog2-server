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
import { WidgetActions } from 'views/stores/WidgetStore';
import MessagesWidget from 'views/logic/widgets/MessagesWidget';

import type { FieldActionHandlerCondition, FieldActionHandler } from './FieldActionHandler';

const RemoveFromTableActionHandler: FieldActionHandler = ({ field, contexts: { widget } }) => {
  const newFields = widget.config.fields.filter((f) => (f !== field));
  const newConfig = widget.config.toBuilder()
    .fields(newFields)
    .build();

  return WidgetActions.updateConfig(widget.id, newConfig);
};

const isEnabled: FieldActionHandlerCondition = ({ contexts: { widget }, field }) => {
  if (MessagesWidget.isMessagesWidget(widget) && widget.config) {
    const fields = widget.config.fields || [];

    return fields.includes(field);
  }

  return false;
};

/* Hide RemoveFromTableHandler in the sidebar */
const isHidden: FieldActionHandlerCondition = ({ contexts: { widget } }): boolean => !widget;

RemoveFromTableActionHandler.isEnabled = isEnabled;
RemoveFromTableActionHandler.isHidden = isHidden;

export default RemoveFromTableActionHandler;
