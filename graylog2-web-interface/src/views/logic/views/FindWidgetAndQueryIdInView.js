// @flow strict
import View from './View';
import Widget from '../widgets/Widget';
import ViewState from './ViewState';

type QueryId = string;

const FindWidgetAndQueryIdInView = (widgetId: string, view: View): ?[Widget, QueryId] => {
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
