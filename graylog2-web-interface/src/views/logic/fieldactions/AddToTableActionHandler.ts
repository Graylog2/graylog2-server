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
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import type Widget from 'views/logic/widgets/Widget';
import type { ActionHandlerCondition, ActionHandlerArguments } from 'views/components/actions/ActionHandler';
import type { AppDispatch } from 'stores/useAppDispatch';
import { updateWidgetConfig } from 'views/logic/slices/widgetActions';

type Contexts = { widget: Widget };

const AddToTableActionHandler = ({
  field,
  contexts: { widget },
}: ActionHandlerArguments<{ widget?: Widget }>) => (dispatch: AppDispatch) => {
  const newFields = [].concat(widget.config.fields, [field]);
  const newConfig = widget.config.toBuilder()
    .fields(newFields)
    .build();

  return dispatch(updateWidgetConfig(widget.id, newConfig));
};

const isEnabled: ActionHandlerCondition<Contexts> = ({ contexts: { widget }, field }) => {
  if (MessagesWidget.isMessagesWidget(widget) && widget.config) {
    const fields = widget.config.fields || [];

    return !fields.includes(field);
  }

  return false;
};

/* Hide AddToTableHandler in the sidebar */
const isHidden: ActionHandlerCondition<Contexts> = ({ contexts: { widget } }) => !widget;

AddToTableActionHandler.isEnabled = isEnabled;
AddToTableActionHandler.isHidden = isHidden;

export default AddToTableActionHandler;
