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
import * as React from 'react';
import { useMemo } from 'react';

import Widget from 'views/logic/widgets/Widget';
import usePluginEntities from 'views/logic/usePluginEntities';
import { MenuItem } from 'components/graylog';

type Props = {
  widget: Widget,
};

type WidgetAction = {
  type: string,
  title: (w: Widget) => string,
  isHidden?: (w: Widget) => boolean,
  action: (w: Widget) => unknown,
};

const ExtraWidgetActions = ({ widget }: Props) => {
  const pluginWidgetActions = usePluginEntities<WidgetAction>('views.widgets.actions');
  const extraWidgetActions = useMemo(() => pluginWidgetActions
    .filter(({ isHidden = () => false }) => !isHidden(widget))
    .map(({ title, action, type }) => <MenuItem key={`${type}-${widget.id}`} onSelect={() => action(widget)}>{title(widget)}</MenuItem>), [pluginWidgetActions, widget]);

  return extraWidgetActions.length > 0
    ? (
      <>
        <MenuItem divider />
        {extraWidgetActions}
      </>
    )
    : null;
};

export default ExtraWidgetActions;
