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
import * as Immutable from 'immutable';

import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import GetPositionForNewWidget from 'views/logic/views/GetPositionForNewWidget';

import Widget from '../widgets/Widget';

const incrementRow = (position: WidgetPosition) => position.row + 1;

export default (
  widgetPositions: Immutable.Map<string, WidgetPosition>,
  widgets: Array<Widget>,
): Immutable.Map<string, WidgetPosition> => Immutable.Map(
  widgets
    .map((widget) => [widget.id, widgetPositions.has(widget.id)
      ? widgetPositions.get(widget.id).toBuilder().row(incrementRow(widgetPositions.get(widget.id))).build()
      : GetPositionForNewWidget(widget)]),
);
