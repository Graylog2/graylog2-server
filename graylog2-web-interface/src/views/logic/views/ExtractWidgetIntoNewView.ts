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
import type { TitleType, TitlesMap } from 'views/stores/TitleTypes';
import ViewState from 'views/logic/views/ViewState';
import type { ViewType } from 'views/logic/views/View';
import View from 'views/logic/views/View';
import type Search from 'views/logic/search/Search';
import type Widget from 'views/logic/widgets/Widget';
import type WidgetPosition from 'views/logic/widgets/WidgetPosition';
import type { WidgetMapping } from 'views/logic/views/types';

const ExtractViewStateDataForWidget = ({ widgetId, view, queryId }) => {
  const currentViewState = view.state.get(queryId);
  const widgetMapping = Immutable.Map({ [widgetId]: currentViewState.widgetMapping.get(widgetId) });
  const titles = Immutable.Map<TitleType, Map<string, string>>({
    widget: Immutable.Map({ [widgetId]: currentViewState.titles.get('widget').get(widgetId) }),
  });
  const widgetPosition = currentViewState.widgetPositions[widgetId].toBuilder().col(0).row(0).build();

  return ({ titles, widgetPosition, widgetMapping });
};

type GetViewStateProps = {
  titles: TitlesMap,
  widget: Widget,
  widgetPosition: WidgetPosition,
  widgetMapping: WidgetMapping,
}
const GetViewState = ({ widget, titles, widgetPosition, widgetMapping }: GetViewStateProps): ViewState => ViewState.create()
  .toBuilder()
  .widgets([widget])
  .titles(titles)
  .widgetPositions({ [widget.id]: widgetPosition })
  .widgetMapping(widgetMapping)
  .build();

type CreateViewWithOneWidgetProps = {
  titles: TitlesMap,
  search: Search,
  widget: Widget,
  widgetPosition: WidgetPosition,
  widgetMapping: WidgetMapping,
  queryId: string,
  viewType?: ViewType,
}

const CreateViewWithOneWidget = ({ titles, search, widget, widgetPosition, widgetMapping, queryId, viewType = View.Type.Search }: CreateViewWithOneWidgetProps) => {
  const viewState = GetViewState({ widget, widgetPosition, widgetMapping, titles });

  return View.create()
    .toBuilder()
    .newId()
    .type(viewType)
    .search(search)
    .state({ [queryId]: viewState })
    .build();
};

const ExtractWidgetIntoNewView = (view: View, widgetId: string, viewType: ViewType = View.Type.Search) => {
  const [widget, queryId] = FindWidgetAndQueryIdInView(widgetId, view);
  const { widgetPosition, widgetMapping, titles } = ExtractViewStateDataForWidget({ widgetId, view, queryId });
  const { search } = view;

  return CreateViewWithOneWidget({ titles, widgetPosition, widget, queryId, widgetMapping, search, viewType });
};

export default ExtractWidgetIntoNewView;
