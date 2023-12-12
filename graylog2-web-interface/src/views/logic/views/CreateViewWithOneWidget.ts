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
import type { Map } from 'immutable';

import FindWidgetAndQueryIdInView from 'views/logic/views/FindWidgetAndQueryIdInView';
import type { TitleType } from 'views/stores/TitleTypes';
import ViewState from 'views/logic/views/ViewState';
import View from 'views/logic/views/View';
import type Widget from 'views/logic/widgets/Widget';

const GetViewState = (view: View, newWidget: Widget, currentQueryId: string): ViewState => {
  const currentViewState = view.state.get(currentQueryId);
  const widgetMappings = Immutable.Map({ [newWidget.id]: currentViewState.widgetMapping.get(newWidget.id) });
  const titles = Immutable.Map<TitleType, Map<string, string>>({
    widget: Immutable.Map({ [newWidget.id]: currentViewState.titles.get('widget').get(newWidget.id) }),
  });
  const widgetPosition = currentViewState.widgetPositions[newWidget.id].toBuilder().col(0).row(0).build();

  return ViewState.create()
    .toBuilder()
    .widgets([newWidget])
    .titles(titles)
    .widgetPositions({ [newWidget.id]: widgetPosition })
    .widgetMapping(widgetMappings)
    .build();
};

const CreateViewWithOneWidget = (view: View, widgetId: string) => {
  const [newWidget, currentQueryId] = FindWidgetAndQueryIdInView(widgetId, view);
  const viewState = GetViewState(view, newWidget, currentQueryId);

  return View.create()
    .toBuilder()
    .newId()
    .type(View.Type.Search)
    .search(view.search)
    .state({ [currentQueryId]: viewState })
    .build();
};

export default CreateViewWithOneWidget;
