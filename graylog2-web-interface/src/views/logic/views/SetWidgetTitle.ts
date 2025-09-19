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
import { Map } from 'immutable';

import type { TitlesMap } from 'views/stores/TitleTypes';
import TitleTypes from 'views/stores/TitleTypes';
import type Widget from 'views/logic/widgets/Widget';

const SetWidgetTitle = (titlesMap: TitlesMap, widget: Widget, title: string) => {
  if (!title) {
    return titlesMap;
  }

  const widgetTitles = titlesMap.get(TitleTypes.Widget, Map());
  const newWidgetTitles = widgetTitles.set(widget.id, title);

  return titlesMap.set(TitleTypes.Widget, newWidgetTitles);
};

export default SetWidgetTitle;
