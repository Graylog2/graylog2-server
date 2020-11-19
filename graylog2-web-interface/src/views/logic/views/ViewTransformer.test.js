/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
// @flow strict
import { readFileSync } from 'fs';

import { List, Map } from 'immutable';
import { dirname } from 'path';

import Search from 'views/logic/search/Search';
import Query from 'views/logic/queries/Query';

import type { ViewStateMap } from './View';
import View from './View';
import ViewState from './ViewState';
import ViewStateGenerator from './ViewStateGenerator';
import viewTransformer from './ViewTransformer';

const mockList = jest.fn(() => Promise.resolve([]));

jest.mock('injection/CombinedProvider', () => ({
  get: (type) => ({
    Decorators: {
      DecoratorsActions: {
        list: (...args) => mockList(...args),
      },
    },
  })[type],
}));

const cwd = dirname(__filename);

const readFixture = (filename) => JSON.parse(readFileSync(`${cwd}/${filename}`).toString());

describe('ViewTransformer', () => {
  describe('transform with missing attributes', () => {
    it('should change the type', () => {
      const query = Query.builder()
        .id('query-id')
        .timerange({ type: 'relative', range: 365 })
        .build();

      const search = Search.builder()
        .id('search-id')
        .queries([query])
        .build();

      const searchView = View.builder()
        .type(View.Type.Search)
        .search(search)
        .build();

      const dashboardView = viewTransformer(searchView);

      expect(dashboardView.type).toBe(View.Type.Dashboard);
    });

    it('should change the id', () => {
      const query = Query.builder()
        .id('query-id')
        .timerange({ type: 'relative', range: 365 })
        .build();

      const search = Search.builder()
        .id('search-id')
        .queries([query])
        .build();

      const searchView = View.builder()
        .id('dead-beef')
        .title('Breq')
        .search(search)
        .type(View.Type.Search)
        .build();

      const dashboardView = viewTransformer(searchView);

      expect(dashboardView.id).not.toStrictEqual(searchView.id);
    });

    it('should add the timerange to the widget', async () => {
      const query = Query.builder()
        .id('query-id')
        .timerange({ type: 'relative', range: 365 })
        .build();

      const search = Search.builder()
        .id('search-id')
        .queries([query])
        .build();

      const viewState: ViewState = await ViewStateGenerator(View.Type.Search);

      const viewStateMap: ViewStateMap = Map({ 'query-id': viewState });
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

    it('should add the query to the widget', async () => {
      const query = Query.builder()
        .id('query-id')
        .query({ type: 'elasticsearch', query_string: 'author: "Karl Marx"' })
        .build();

      const search = Search.builder()
        .id('search-id')
        .queries([query])
        .build();

      const viewState: ViewState = await ViewStateGenerator(View.Type.Search);

      const viewStateMap: ViewStateMap = Map({ 'query-id': viewState });
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

    it('should add the streams to the widget', async () => {
      const query = Query.builder()
        .id('query-id')
        .filter(Map({ type: 'or', filters: List([Map({ type: 'stream', id: '1234-abcd' })]) }))
        .build();

      const search = Search.builder()
        .id('search-id')
        .queries([query])
        .build();

      const viewState: ViewState = await ViewStateGenerator(View.Type.Search);

      const viewStateMap: ViewStateMap = Map({ 'query-id': viewState });
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

    it('should remove the query_string from search queries', async () => {
      const query = Query.builder()
        .id('query-id')
        .query({ type: 'elasticsearch', query_string: 'author: "Karl Marx"' })
        .build();

      const search = Search.builder()
        .id('search-id')
        .queries([query])
        .build();

      const searchView = View.builder()
        .type(View.Type.Search)
        .search(search)
        .build();

      const dashboardView = viewTransformer(searchView);

      expect(dashboardView.search.queries.first().query).toEqual({ type: 'elasticsearch', query_string: '' });
    });
  });

  describe('transform with all attributes', () => {
    it('should transform a view with search from a json fixture', () => {
      const viewFixture = View.fromJSON(readFixture('./ViewTransformer.view.fixture.json'));
      const searchFixture = Search.fromJSON(readFixture('./ViewTransformer.search.fixture.json'));
      const searchView = viewFixture.toBuilder()
        .search(searchFixture)
        .build();
      const dashboardView = viewTransformer(searchView);

      expect(dashboardView).toMatchSnapshot({
        _value: {
          id: expect.any(String),
        },
      });
    });
  });
});
