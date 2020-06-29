// @flow strict
import { List, Map } from 'immutable';

import Widget from 'views/logic/widgets/Widget';
import type { QueryId } from 'views/logic/queries/Query';
import Query from 'views/logic/queries/Query';

import type { ViewStateMap } from './View';
import View from './View';
import ViewState from './ViewState';

const ViewTransformer = (searchView: View): View => {
  const queryMap: Map<QueryId, Query> = Map(searchView.search.queries.map((q) => [q.id, q]));
  const newViewStateMap: ViewStateMap = (searchView.state || Map()).map((viewState: ViewState, queryId: string) => {
    const { timerange, query, filter = Map() } = queryMap.get(queryId);

    const streams = (filter ? filter.get('filters', List()) : List())
      .filter((value) => Map.isMap(value) && value.get('type') === 'stream')
      .map((value) => value.get('id'))
      .toList()
      .toArray();

    const widgets: List<Widget> = viewState.widgets.map((widget: Widget) => {
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
  // Remove query string attached to the existing search query
  const newQueries = searchView.search.queries.map(
    (query) => query.toBuilder().query({ ...query.query, query_string: '' }).build(),
  );
  const newSearch = searchView.search.toBuilder().queries(newQueries).build();

  return searchView.toBuilder()
    .newId()
    .type(View.Type.Dashboard)
    .state(newViewStateMap)
    .search(newSearch)
    .build();
};

export default ViewTransformer;
