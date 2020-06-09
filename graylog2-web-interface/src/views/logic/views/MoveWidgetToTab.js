// @flow strict
import uuid from 'uuid/v4';
import type { QueryId } from 'views/logic/queries/Query';
import type { WidgetId } from 'views/logic/views/types';
import View from './View';
import FindWidgetAndQueryIdInView from './FindWidgetAndQueryIdInView';
import Widget from '../widgets/Widget';
import UpdateSearchForWidgets from './UpdateSearchForWidgets';
import WidgetPosition from '../widgets/WidgetPosition';


const _removeWidgetFromTab = (widgetId: WidgetId, queryId: QueryId, dashboard: View): View => {
  const viewState = dashboard.state.get(queryId);
  const widgetIndex = viewState.widgets.findIndex((widget) => widget.id === widgetId);
  const { widgetPositions } = viewState;
  delete widgetPositions[widgetId];
  const { widgetMapping } = viewState;
  const newWidgetMapping = widgetMapping.remove(widgetId);
  const newViewState = viewState.toBuilder()
    .widgets(viewState.widgets.delete(widgetIndex))
    .widgetMapping(newWidgetMapping)
    .widgetPositions(widgetPositions)
    .build();
  return dashboard.toBuilder()
    .state(dashboard.state.set(queryId, newViewState))
    .build();
};

const _addWidgetToTab = (widget: Widget, targetQueryId: QueryId, dashboard: View, widgetPosition: WidgetPosition): View => {
  const viewState = dashboard.state.get(targetQueryId);
  const newWidget = widget.toBuilder().id(uuid()).build();
  const { widgetPositions } = viewState;
  widgetPositions[newWidget.id] = widgetPosition;
  const newViewState = viewState.toBuilder()
    .widgets(viewState.widgets.push(newWidget))
    .widgetPositions(widgetPositions)
    .build();
  return dashboard.toBuilder()
    .state(dashboard.state.set(targetQueryId, newViewState))
    .build();
};

const _getWidgetPosition = (widgetId: WidgetId, queryId: QueryId, view: View): WidgetPosition => {
  return view.state.get(queryId).widgetPositions[widgetId];
};

const MoveWidgetToTab = (widgetId: WidgetId, targetQueryId: QueryId, dashboard: View, copy: boolean = false): ?View => {
  if (dashboard.type !== View.Type.Dashboard) {
    throw new Error(`Unexpected type ${dashboard.type} expected ${View.Type.Dashboard}`);
  }

  const match: ?[Widget, QueryId] = FindWidgetAndQueryIdInView(widgetId, dashboard);

  if (match) {
    const [widget, queryId] = match;
    const newWidgetPosition = _getWidgetPosition(widgetId, queryId, dashboard).toBuilder()
      .col(1)
      .row(1)
      .build();
    const tempDashboard = copy ? dashboard : _removeWidgetFromTab(widgetId, queryId, dashboard);
    return UpdateSearchForWidgets(_addWidgetToTab(widget, targetQueryId, tempDashboard, newWidgetPosition));
  }
  return undefined;
};

export default MoveWidgetToTab;
