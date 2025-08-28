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

import type WidgetPosition from 'views/logic/widgets/WidgetPosition';
import GetPositionForNewWidget from 'views/logic/views/GetPositionForNewWidget';

import type Widget from '../widgets/Widget';

export const ConcatPositions = (
  newPositions: Immutable.Map<string, WidgetPosition>,
  curPositions: Immutable.Map<string, WidgetPosition>,
) => {
  let rowIncrement = 0;

  const newUpdatedPositions = newPositions
    .map((initialPosition) => {
      const defaultHeight = initialPosition.height;
      const row = rowIncrement + initialPosition.row;
      const widgetPosition = initialPosition.toBuilder().row(row).build();
      rowIncrement += defaultHeight;

      return widgetPosition;
    })
    .toMap();

  const curUpdatedPositions = curPositions
    .map((initialPosition) => {
      const row = rowIncrement + initialPosition.row;

      return initialPosition.toBuilder().row(row).build();
    })
    .toMap();

  return newUpdatedPositions.merge(curUpdatedPositions);
};

export default (
  widgetPositions: Immutable.Map<string, WidgetPosition>,
  widgets: Array<Widget>,
  height: number = undefined,
  width: number = undefined,
): Immutable.Map<string, WidgetPosition> => {
  const widgetsWithoutPosition = widgets.filter((widget) => !widgetPositions.has(widget.id));

  const newPositions: Immutable.Map<string, WidgetPosition> = Immutable.Map(
    widgetsWithoutPosition.map((widget) => [
      widget.id,
      GetPositionForNewWidget(widget, widgetPositions, height, width),
    ]),
  );

  return widgetPositions.merge(newPositions);
};
