// @flow strict
import * as Immutable from 'immutable';
import uuid from 'uuid/v4';
import type { QueryId } from 'views/logic/queries/Query';
import type { WidgetId } from 'views/logic/views/types';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import type { TitlesMap } from 'views/stores/TitleTypes';
import View from './View';
import FindWidgetAndQueryIdInView from './FindWidgetAndQueryIdInView';
import Widget from '../widgets/Widget';
import UpdateSearchForWidgets from './UpdateSearchForWidgets';
import AddNewWidgetsToPositions from './AddNewWidgetsToPositions';

const _removeWidgetTitle = (titlesMap: TitlesMap, widgetId: WidgetId): TitlesMap => {
  const widgetTitles = titlesMap.get('widget');
  if (!widgetTitles) {
    return titlesMap;
  }
  const newWidgetTitles = widgetTitles.remove(widgetId);
  return titlesMap.set('widget', newWidgetTitles);
};

const _removeWidgetFromTab = (widgetId: WidgetId, queryId: QueryId, dashboard: View): View => {
  const viewState = dashboard.state.get(queryId);
  const widgetIndex = viewState.widgets.findIndex((widget) => widget.id === widgetId);
  const { widgetPositions, titles } = viewState;
  const newTitles = _removeWidgetTitle(titles, widgetId);
  delete widgetPositions[widgetId];
  const { widgetMapping } = viewState;
  const newWidgetMapping = widgetMapping.remove(widgetId);
  const newViewState = viewState.toBuilder()
    .widgets(viewState.widgets.delete(widgetIndex))
    .widgetMapping(newWidgetMapping)
    .titles(newTitles)
    .widgetPositions(widgetPositions)
    .build();
  return dashboard.toBuilder()
    .state(dashboard.state.set(queryId, newViewState))
    .build();
};

const _setWidgetTitle = (titlesMap: TitlesMap, widgetID: WidgetId, newTitle: ?string): TitlesMap => {
  if (!newTitle) {
    return titlesMap;
  }
  const widgetTitlesMap = titlesMap.get('widget', Immutable.Map());
  const newWidgetTitleMap = widgetTitlesMap.set(widgetID, newTitle);
  return titlesMap.set('widget', newWidgetTitleMap);
};

const _addWidgetToTab = (widget: Widget, targetQueryId: QueryId, dashboard: View, newWidgetPosition: WidgetPosition, widgetTitle: ?string): View => {
  const viewState = dashboard.state.get(targetQueryId);
  const newWidget = widget.toBuilder().id(uuid()).build();
  const newWidgets = viewState.widgets.push(newWidget);
  const overridePositions = Immutable.Map({ [newWidget.id]: newWidgetPosition });
  const { widgetPositions } = viewState;
  const newWidgetPositions = AddNewWidgetsToPositions(Immutable.Map(widgetPositions), newWidgets.toArray(), overridePositions);
  const newTitleMap = _setWidgetTitle(viewState.titles, newWidget.id, widgetTitle);
  const newViewState = viewState.toBuilder()
    .widgets(newWidgets)
    .titles(newTitleMap)
    .widgetPositions(newWidgetPositions)
    .build();
  return dashboard.toBuilder()
    .state(dashboard.state.set(targetQueryId, newViewState))
    .build();
};

const _getWidgetPosition = (widgetId: WidgetId, queryId: QueryId, view: View): WidgetPosition => {
  return view.state.get(queryId).widgetPositions[widgetId];
};

const _getWidgetTitle = (widgetId: WidgetId, queryId: QueryId, view: View): ?string => {
  return view.state.get(queryId).titles.get('widget').get(widgetId);
};

const MoveWidgetToTab = (widgetId: WidgetId, targetQueryId: QueryId, dashboard: View, copy: boolean = false): ?View => {
  if (dashboard.type !== View.Type.Dashboard) {
    throw new Error(`Unexpected type ${dashboard.type} expected ${View.Type.Dashboard}`);
  }

  const match: ?[Widget, QueryId] = FindWidgetAndQueryIdInView(widgetId, dashboard);

  if (match) {
    const [widget, queryId] = match;
    const widgetTitle = _getWidgetTitle(widgetId, queryId, dashboard);
    const newWidgetPosition = _getWidgetPosition(widgetId, queryId, dashboard).toBuilder()
      .col(1)
      .row(1)
      .build();
    const tempDashboard = copy ? dashboard : _removeWidgetFromTab(widgetId, queryId, dashboard);
    return UpdateSearchForWidgets(_addWidgetToTab(widget, targetQueryId, tempDashboard, newWidgetPosition, widgetTitle));
  }
  return undefined;
};

export default MoveWidgetToTab;
