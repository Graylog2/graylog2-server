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

const GetPositionForNewWidget = (widget: Widget) => {
  const { defaultHeight, defaultWidth } = widgetDefinition(widget.type);

  return WidgetPosition.builder()
    .height(defaultHeight)
    .width(defaultWidth)
    .col(1)
    .row(1)
    .build();
};

export default GetPositionForNewWidget;
