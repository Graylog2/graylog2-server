// @flow strict
import { List, Map } from 'immutable';

import Widget from 'views/logic/widgets/Widget';
import View from 'views/logic/views/View';
import ViewState from 'views/logic/views/ViewState';
import Query from 'views/logic/queries/Query';

import UpdateSearchForWidgets from './UpdateSearchForWidgets';

type QueryId = string;

const _findWidgetAndQueryIdInView = (widgetId: string, view: View): ?[Widget, QueryId] => {
  return view.state.reduce((foundWidget: ?[Widget, QueryId], state: ViewState, queryId: QueryId): ?[Widget, QueryId] => {
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

const _addWidgetToDashboard = (widget: Widget, dashboard: View): View => {
  const dashboardQueryId = dashboard.state.keySeq().first();
  const viewState = dashboard.state.get(dashboardQueryId);
  const newViewState = viewState.toBuilder()
    .widgets(viewState.widgets.push(widget))
    .build();
  return dashboard.toBuilder()
    .state(dashboard.state.set(dashboardQueryId, newViewState))
    .build();
};

const CopyWidgetToDashboard = (widgetId: string, search: View, dashboard: View): ?View => {
  if (dashboard.type !== View.Type.Dashboard) {
    return undefined;
  }

  const queryMap: Map<QueryId, Query> = Map(search.search.queries.map((q) => [q.id, q]));
  const match: ?[Widget, QueryId] = _findWidgetAndQueryIdInView(widgetId, search);

  if (match) {
    const [widget, queryId] = match;
    const { timerange, query, filter = Map() } = queryMap.get(queryId);

    const streams = (filter ? filter.get('filters', List.of()) : List.of())
      .filter((value) => Map.isMap(value) && value.get('type') === 'stream')
      .map((value) => value.get('id'))
      .toList()
      .toArray();

    const dashboardWidget = widget.toBuilder()
      .timerange(timerange)
      .query(query)
      .streams(streams)
      .build();

    return UpdateSearchForWidgets(_addWidgetToDashboard(dashboardWidget, dashboard));
  }

  return undefined;
};

export default CopyWidgetToDashboard;
