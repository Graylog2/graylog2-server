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
      const streams = filter
        .filter(value => List.isList(value))
        .map(filterList => filterList
          .filter(value => Map.isMap(value) && value.get('type') === 'stream')
          .map(value => value.get('id'))
          .toList())
        .toList()
        .flatten(true)
        .toArray();

      return widget.toBuilder()
        .timerange(timerange)
        .query(query)
        .streams(streams)
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
