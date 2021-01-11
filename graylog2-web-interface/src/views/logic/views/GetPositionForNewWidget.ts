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
import { widgetDefinition } from 'views/logic/Widgets';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import Widget from 'views/logic/widgets/Widget';
import View from 'views/logic/views/View';

const GetPositionForNewWidget = (widget: Widget, queryId: string, view: View) => {
  const widgetDef = widgetDefinition(widget.type);

  const widgetPositionsBuilder = view.state.get(queryId).widgetPositions[widget.id]?.toBuilder()
    || WidgetPosition.builder().width(widgetDef.defaultWidth).height(widgetDef.defaultHeight);

  return widgetPositionsBuilder.col(1).row(1).build();
};

export default GetPositionForNewWidget;
