// @flow strict
import * as Immutable from 'immutable';
import View from './View';
import FindWidgetAndQueryIdInView from './FindWidgetAndQueryIdInView';
import Widget from '../widgets/Widget';
import UpdateSearchForWidgets from './UpdateSearchForWidgets';

type QueryId = string;

const _removeWidgetFromTab = (widgetId: string, queryId: string, dashboard: View): View => {
  const viewState = dashboard.state.get(queryId);
  const widgetIndex = viewState.widgets.findIndex((widget) => widget.id === widgetId);
  const widgetPosition = viewState.widgetPositions;
  delete widgetPosition[widgetId];
  const { widgetMapping } = viewState;
  const newWidgetMapping = widgetMapping.remove(widgetId);
  const newViewState = viewState.toBuilder()
    .widgets(viewState.widgets.delete(widgetIndex))
    .widgetMapping(newWidgetMapping)
    .widgetPositions(widgetPosition)
    .build();
  return dashboard.toBuilder()
    .state(dashboard.state.set(queryId, newViewState))
    .build();
};

const _addWidgetToTab = (widget: Widget, targetQueryId: QueryId, dashboard: View, searchTypeIds: Immutable.Set<string>): View => {
  const viewState = dashboard.state.get(targetQueryId);
  const newViewState = viewState.toBuilder()
    .widgets(viewState.widgets.push(widget))
    .build();
  return dashboard.toBuilder()
    .state(dashboard.state.set(targetQueryId, newViewState))
    .build();
};

const MoveWidgetToTab = (widgetId: string, targetQueryId: QueryId, dashboard: View, copy: boolean = false): ?View => {
  if (dashboard.type !== View.Type.Dashboard) {
    return undefined;
  }

  const match: ?[Widget, QueryId] = FindWidgetAndQueryIdInView(widgetId, dashboard);

  if (match) {
    const [widget, queryId] = match;
    const searchTypes = dashboard.state.get(queryId).widgetMapping.get(widgetId);
    const tempDashboard = copy ? dashboard : _removeWidgetFromTab(widgetId, queryId, dashboard);
    return UpdateSearchForWidgets(_addWidgetToTab(widget, targetQueryId, tempDashboard, searchTypes));
  }
  return undefined;
};

export default MoveWidgetToTab;
