// @flow strict
import type { QueryId } from 'views/logic/queries/Query';
import type { WidgetId } from 'views/logic/views/types';

import View from './View';
import Widget from '../widgets/Widget';
import ViewState from './ViewState';

const FindWidgetAndQueryIdInView = (widgetId: WidgetId, view: View): ?[Widget, QueryId] => {
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

export default FindWidgetAndQueryIdInView;
