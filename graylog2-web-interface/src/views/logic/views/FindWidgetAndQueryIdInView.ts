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
import type { QueryId } from 'views/logic/queries/Query';
import type { WidgetId } from 'views/logic/views/types';

import type View from './View';
import type ViewState from './ViewState';

import type Widget from '../widgets/Widget';

const FindWidgetAndQueryIdInView = (widgetId: WidgetId, view: View): [Widget, QueryId] | undefined | null => {
  return view.state.reduce((foundWidget: [Widget, QueryId] | undefined | null, state: ViewState, queryId: QueryId): [Widget, QueryId] | undefined | null => {
    if (foundWidget) {
      return foundWidget;
    }

    const widget = state.widgets.find((w) => w.id === widgetId);

    if (widget) {
      return [widget, queryId];
    }

    return undefined;
  }, undefined);
};

export default FindWidgetAndQueryIdInView;
