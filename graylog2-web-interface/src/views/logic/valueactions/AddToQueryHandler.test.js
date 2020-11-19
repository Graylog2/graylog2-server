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
import * as Immutable from 'immutable';
import mockAction from 'helpers/mocking/MockAction';
import asMock from 'helpers/mocking/AsMock';

import { QueriesActions, QueriesStore } from 'views/stores/QueriesStore';
import { ViewStore } from 'views/stores/ViewStore';
import FieldType from 'views/logic/fieldtypes/FieldType';
import Query from 'views/logic/queries/Query';
import { GlobalOverrideActions, GlobalOverrideStore } from 'views/stores/GlobalOverrideStore';
import SearchActions from 'views/actions/SearchActions';

import AddToQueryHandler from './AddToQueryHandler';

import View from '../views/View';
import GlobalOverride from '../search/GlobalOverride';

jest.mock('views/stores/QueriesStore', () => ({ QueriesStore: {}, QueriesActions: {} }));
jest.mock('views/stores/ViewStore', () => ({ ViewStore: {} }));
jest.mock('views/stores/GlobalOverrideStore', () => ({ GlobalOverrideStore: {}, GlobalOverrideActions: {} }));
jest.mock('views/actions/SearchActions', () => ({}));

describe('AddToQueryHandler', () => {
  const view = View.create().toBuilder().type(View.Type.Search).build();
  let queriesStoreListen;
  let queries;

  beforeEach(() => {
    QueriesStore.listen = jest.fn((cb) => {
      queriesStoreListen = cb;

      return () => {};
    });

    QueriesStore.getInitialState = jest.fn(() => queries);
    QueriesActions.query = mockAction(jest.fn(() => Promise.resolve()));
    ViewStore.listen = jest.fn(() => () => {});

    ViewStore.getInitialState = jest.fn(() => ({
      view,
      activeQuery: 'queryId',
      dirty: false,
      isNew: false,
    }));

    GlobalOverrideStore.listen = jest.fn(() => () => {});
    GlobalOverrideStore.getInitialState = jest.fn(() => undefined);
  });

  it('formats date field for ES', () => {
    const query = Query.builder()
      .query({ type: 'elasticsearch', query_string: '' })
      .build();

    queries = Immutable.OrderedMap({ queryId: query });

    const addToQueryHandler = new AddToQueryHandler();

    return addToQueryHandler.handle({
      queryId: 'queryId',
      field: 'timestamp',
      value: '2019-01-17T11:00:09.025Z',
      type: new FieldType('date', [], []),
      contexts: { view },
    })
      .then(() => {
        expect(QueriesActions.query).toHaveBeenCalledWith('queryId', 'timestamp:"2019-01-17 11:00:09.025"');
      });
  });

  it('updates query string before adding predicate', () => {
    const query = Query.builder()
      .query({ type: 'elasticsearch', query_string: '' })
      .build();

    queries = Immutable.OrderedMap({ queryId: query });

    const addToQueryHandler = new AddToQueryHandler();

    const newQuery = Query.builder()
      .query({ type: 'elasticsearch', query_string: 'foo:23' })
      .build();

    queriesStoreListen(Immutable.OrderedMap({ anotherQueryId: newQuery }));

    return addToQueryHandler.handle({ queryId: 'anotherQueryId', field: 'bar', value: 42, type: new FieldType('keyword', [], []), contexts: { view } })
      .then(() => {
        expect(QueriesActions.query).toHaveBeenCalledWith('anotherQueryId', 'foo:23 AND bar:42');
      });
  });

  describe('for dashboards', () => {
    beforeEach(() => {
      asMock(ViewStore.getInitialState).mockReturnValue({
        view: View.builder().type(View.Type.Dashboard).build(),
        activeQuery: 'queryId',
        dirty: false,
        isNew: false,
      });

      asMock(GlobalOverrideStore.getInitialState).mockReturnValue(GlobalOverride.empty()
        .toBuilder()
        .query({ type: 'elasticsearch', query_string: 'something' })
        .build());

      GlobalOverrideActions.query = mockAction(jest.fn(() => Promise.resolve(undefined)));
      SearchActions.refresh = mockAction(jest.fn(() => Promise.resolve()));
    });

    it('retrieves query string from global override', () => {
      const addToQueryHandler = new AddToQueryHandler();

      return addToQueryHandler.handle({ queryId: 'queryId', field: 'bar', value: 42, type: new FieldType('keyword', [], []), contexts: { view } })
        .then(() => {
          expect(GlobalOverrideActions.query).toHaveBeenCalledWith('something AND bar:42');
          expect(SearchActions.refresh).toHaveBeenCalled();
        });
    });
  });
});
