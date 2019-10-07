// @flow strict
import { Map } from 'immutable';
import Search from 'views/logic/search/Search';
import Query from 'views/logic/queries/Query';
import type { ViewStateMap } from './View';
import View from './View';
import viewTransformer from './ViewTransformer';
import ViewState from './ViewState';
import ViewStateGenerator from './ViewStateGenerator';

describe('ViewTransformer', () => {
  it('should change the type', () => {
    const searchView = View.builder()
      .type(View.Type.Search)
      .build();

    const dashboardView = viewTransformer(searchView);
    expect(dashboardView.type).toBe(View.Type.Dashboard);
  });

  it('should add the timerange to the widget', () => {
    const query = Query.builder()
      .id('query-id')
      .timerange({ type: 'relative', range: 365 })
      .build();

    const search = Search.builder()
      .id('search-id')
      .queries([query])
      .build();

    const viewState: ViewState = ViewStateGenerator(View.Type.Search);

    const viewStateMap: ViewStateMap = Map.of('query-id', viewState);
    const searchView = View.builder()
      .type(View.Type.Search)
      .state(viewStateMap)
      .search(search)
      .build();
    const dashboardView = viewTransformer(searchView);
    expect(dashboardView.state.get('query-id').widgets.first().timerange).toBe(query.timerange);
    expect(dashboardView.state.get('query-id').widgets.first().query).toBeUndefined();
    expect(dashboardView.state.get('query-id').widgets.first().streams).toStrictEqual([]);
  });

  it('should add the query to the widget', () => {
    const query = Query.builder()
      .id('query-id')
      .query({ type: 'elasticsearch', query_string: 'author: "Karl Marx"' })
      .build();

    const search = Search.builder()
      .id('search-id')
      .queries([query])
      .build();

    const viewState: ViewState = ViewStateGenerator(View.Type.Search);

    const viewStateMap: ViewStateMap = Map.of('query-id', viewState);
    const searchView = View.builder()
      .type(View.Type.Search)
      .state(viewStateMap)
      .search(search)
      .build();
    const dashboardView = viewTransformer(searchView);
    expect(dashboardView.state.get('query-id').widgets.first().timerange).toBeUndefined();
    expect(dashboardView.state.get('query-id').widgets.first().query).toBe(query.query);
    expect(dashboardView.state.get('query-id').widgets.first().streams).toStrictEqual([]);
  });

  it('should add the streams to the widget', () => {
    const query = Query.builder()
      .id('query-id')
      .filter({ type: 'or', filters: { type: 'stream', id: '1234-abcd' } })
      .build();

    const search = Search.builder()
      .id('search-id')
      .queries([query])
      .build();

    const viewState: ViewState = ViewStateGenerator(View.Type.Search);

    const viewStateMap: ViewStateMap = Map.of('query-id', viewState);
    const searchView = View.builder()
      .type(View.Type.Search)
      .state(viewStateMap)
      .search(search)
      .build();
    const dashboardView = viewTransformer(searchView);
    expect(dashboardView.state.get('query-id').widgets.first().timerange).toBeUndefined();
    expect(dashboardView.state.get('query-id').widgets.first().query).toBeUndefined();
    expect(dashboardView.state.get('query-id').widgets.first().streams).toStrictEqual(['1234-abcd']);
  });
});
