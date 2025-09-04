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
import type * as Immutable from 'immutable';

import { widgetDefinition } from 'views/logic/Widgets';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import type Widget from 'views/logic/widgets/Widget';

const GetPositionForNewWidget = (
  widget: Widget,
  widgetPositions: Immutable.Map<string, WidgetPosition>,
  height: number = undefined,
  width: number = undefined,
) => {
  const { defaultHeight, defaultWidth } = widgetDefinition(widget.type);

  const newRow = (widgetPositions.map((position) => position.row + position.height).max() ?? 0) + 1;

  return WidgetPosition.builder()
    .height(height ?? defaultHeight)
    .width(width ?? defaultWidth)
    .col(1)
    .row(newRow)
    .build();
};

export default GetPositionForNewWidget;
