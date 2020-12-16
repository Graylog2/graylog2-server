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
import * as Immutable from 'immutable';
import mockAction from 'helpers/mocking/MockAction';

import ViewState from 'views/logic/views/ViewState';
import SearchActions from 'views/actions/SearchActions';
import Search from 'views/logic/search/Search';
import Query from 'views/logic/queries/Query';
import View from 'views/logic/views/View';

import { ViewActions, ViewStore } from './ViewStore';

import AggregationWidget from '../logic/aggregationbuilder/AggregationWidget';

jest.mock('views/actions/SearchActions');

jest.mock('views/logic/Widgets', () => ({
  widgetDefinition: () => ({
    searchTypes: () => [],
  }),
  resultHistogram: () => ({}),
  allMessagesTable: () => ({}),
}));

jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));

describe('ViewStore', () => {
  const dummyState = Immutable.OrderedMap({
    firstQueryId: ViewState.builder().build(),
    secondQueryId: ViewState.builder().build(),
  });
  const dummyView = View.builder().state(dummyState).build();

  it('.load should select first query if activeQuery is not set', () => ViewActions.load(dummyView)
    .then((state) => expect(state.activeQuery).toBe('firstQueryId')));

  it('.load should set isNew to false', () => ViewActions.load(dummyView)
    .then((state) => expect(state.isNew).toBeFalsy()));

  it('.load should select activeQuery if it is set and present in view', () => ViewActions.selectQuery('secondQueryId')
    .then(() => ViewActions.load(dummyView))
    .then((state) => expect(state.activeQuery).toBe('secondQueryId')));

  it('.load should select first query if activeQuery is set but not present in view', () => ViewActions.selectQuery('nonExistingQueryId')
    .then(() => ViewActions.load(dummyView))
    .then((state) => expect(state.activeQuery).toBe('firstQueryId')));

  it('.load should not mark isNew as true by default', () => ViewActions.selectQuery('nonExistingQueryId')
    .then(() => ViewActions.load(dummyView))
    .then((state) => expect(state.isNew).toBe(false)));

  it('.load should allow to mark isNew as true', () => ViewActions.selectQuery('nonExistingQueryId')
    .then(() => ViewActions.load(dummyView, true))
    .then((state) => expect(state.isNew).toBe(true)));

  it('.update should update existing view state', () => {
    // const search = Search.create();
    const newView = View.builder()
      .newId()
      .title('Initial title')
      .description('Initail description')
      .state(dummyState)
      .summary('Initial summary')
      .build();

    return ViewActions.load(newView)
      .then(({ view }) => {
        const updatedView = view.toBuilder()
          .title('New title')
          .description('New description')
          .summary('New summary')
          .build();

        return ViewActions.update(updatedView);
      }).then(() => {
        const state = ViewStore.getInitialState();

        expect(state.view.title).toEqual('New title');
        expect(state.view.description).toEqual('New description');
        expect(state.view.summary).toEqual('New summary');
      });
  });

  describe('maintains dirty flag:', () => {
    beforeEach(() => {
      SearchActions.create = mockAction(jest.fn((s) => Promise.resolve({ search: s })));
    });

    it('resets dirty flag when an existing view is updated', () => {
      const search = Search.create();
      const newView = View.builder().newId()
        .state(dummyState)
        .search(search)
        .build();

      return ViewActions.load(newView)
        .then(() => ViewActions.search(search.toBuilder().newId().build()))
        .then((view) => {
          const state = ViewStore.getInitialState();

          expect(state.dirty).toEqual(true);

          return ViewActions.update(view);
        })
        .then(() => {
          const state = ViewStore.getInitialState();

          expect(state.dirty).toEqual(false);
        });
    });

    it('sets dirty flag to false when creating a view', () => {
      return ViewActions.create(View.Type.Dashboard)
        .then(({ dirty }) => expect(dirty).toBeFalsy());
    });

    it('sets isNew flag to true when creating a view', () => {
      return ViewActions.create(View.Type.Dashboard)
        .then(({ isNew }) => expect(isNew).toBeTruthy());
    });
  });

  describe('search recreation:', () => {
    let search: Search;
    let view: View;

    beforeEach(() => {
      SearchActions.create = mockAction(jest.fn((s) => Promise.resolve({ search: s })));

      search = Search.create()
        .toBuilder()
        .queries([Query.builder().id('firstQueryId').build()])
        .build();

      view = View.create()
        .toBuilder()
        .state(Immutable.fromJS({ firstQueryId: ViewState.create() }))
        .search(search)
        .build();

      return ViewActions.load(view);
    });

    it('should create search when view is created', () => ViewActions.create(View.Type.Dashboard)
      .then(() => expect(SearchActions.create).toHaveBeenCalled()));

    it('should create search when state is updated', () => {
      const newState = Immutable.fromJS({
        firstQueryId: ViewState.create()
          .toBuilder()
          .widgets(Immutable.fromJS([AggregationWidget.builder().build()]))
          .build(),
      });

      return ViewActions.state(newState)
        .then(() => expect(SearchActions.create).toHaveBeenCalled());
    });

    it('should not recreate search when state is updated to identical state', () => ViewActions.state(view.state)
      .then(() => expect(SearchActions.create).not.toHaveBeenCalled()));

    it('should create search when search is replaced', () => {
      const newSearch = search.toBuilder().newId().build();

      return ViewActions.search(newSearch)
        .then(() => expect(SearchActions.create).toHaveBeenCalled());
    });
  });
});
