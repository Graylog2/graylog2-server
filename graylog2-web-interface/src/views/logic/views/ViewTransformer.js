// @flow strict
import { List, Map } from 'immutable';
import Widget from 'views/logic/widgets/Widget';

import type { ViewStateMap } from './View';
import View from './View';
import ViewState from './ViewState';

const ViewTransformer = (searchView: View): View => {
  const newViewStateMap: ViewStateMap = (searchView.state || Map.of()).map((viewState: ViewState, queryId: string) => {
    const { timerange, query, filter = List.of() } = searchView.search.queries.find(q => q.id === queryId);

    const widgets: List<Widget> = viewState.widgets.map((widget: Widget) => {
      return widget.toBuilder()
        .timerange(timerange)
        .query(query)
        .streams(filter.filter(value => value instanceof Map && value.get('type') === 'stream')
          .map(value => value.get('id')).toArray())
        .build();
    });
    return viewState.toBuilder()
      .widgets(widgets)
      .build();
  });

  return searchView.toBuilder()
    .type(View.Type.Dashboard)
    .state(newViewStateMap)
    .build();
};

export default ViewTransformer;
