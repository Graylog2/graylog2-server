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
import * as Immutable from 'immutable';

import { widgetDefinition } from 'views/logic/Widgets';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';

import Widget from '../widgets/Widget';

export default (positionsMap: Immutable.Map<string, WidgetPosition>,
  widgets: Array<Widget>,
  overrideWidgetPositions: Immutable.Map<string, WidgetPosition> = Immutable.Map()): Immutable.Map<string, WidgetPosition> => {
  const newWidgets = widgets.filter((widget) => !positionsMap.get(widget.id));

  return newWidgets.reduce((nextPositionsMap, widget) => {
    const widgetDef = widgetDefinition(widget.type);
    const result = nextPositionsMap.reduce((newPosMap, position, id) => {
      const pos = position.toBuilder().row(position.row + widgetDef.defaultHeight).build();

      return newPosMap.set(id, pos);
    }, Immutable.Map());
    const position = overrideWidgetPositions.get(widget.id, new WidgetPosition(1, 1, widgetDef.defaultHeight, widgetDef.defaultWidth));

    return result.set(widget.id, position.toBuilder().row(1).col(1).build());
  }, positionsMap);
};
