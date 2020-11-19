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
import moment from 'moment';
import * as Immutable from 'immutable';
import asMock from 'helpers/mocking/AsMock';

import { QueriesActions, QueriesStore } from './QueriesStore';
import { ViewStore } from './ViewStore';

import type { QueryId } from '../logic/queries/Query';
import Query from '../logic/queries/Query';
import Search from '../logic/search/Search';

jest.mock('./ViewStore', () => ({
  ViewStore: {
    listen: jest.fn(() => {}),
  },
  ViewActions: {
    search: jest.fn(() => Promise.resolve()),
  },
}));

jest.mock('./ViewStatesStore', () => ({
  ViewStatesActions: {},
}));

const fixedDate = new Date('2020-01-16T12:23:42.123Z');

jest.spyOn(global.Date, 'now')
  .mockReturnValue(fixedDate.valueOf());

describe('QueriesStore', () => {
  beforeEach(() => {
    jest.resetModules();
  });

  it('initializes with an empty map', () => {
    expect(QueriesStore.getInitialState()).toEqual(new Immutable.OrderedMap<QueryId, Query>());
  });

  it('subscribes to ViewStore upon initialization', () => {
    expect(ViewStore.listen).toHaveBeenCalled();
  });

  it('retrieves and propagates queries from ViewStore upon updates', (done) => {
    const query1 = Query.builder().id('query1').build();
    const queries = [query1];
    const callback = asMock(ViewStore.listen).mock.calls[0][0];
    const unsubscribe = QueriesStore.listen((newQueries) => {
      expect(newQueries).toEqual(Immutable.OrderedMap<QueryId, Query>({ query1 }));

      done();
    });

    callback({
      view: {
        search: {
          queries,
        },
      },
    });

    unsubscribe();
  });

  describe('rangeType', () => {
    const query1 = Query.builder()
      .id('query1')
      .timerange({ type: 'relative', range: 300 })
      .build();
    const queries = [query1];

    beforeEach(() => {
      const callback = asMock(ViewStore.listen).mock.calls[0][0];

      callback({
        view: {
          search: Search.builder().queries(queries).build(),
        },
      });
    });

    it('throws error if no type is given', () => {
      // $FlowFixMe: Passing no second argument on purpose
      return QueriesActions.rangeType('query1', undefined)
        .catch((error) => expect(error).toEqual(new Error('Invalid time range type: undefined')));
    });

    it('throws error if invalid type is given', () => {
      // $FlowFixMe: Passing invalid second argument on purpose
      return QueriesActions.rangeType('query1', 'invalid')
        .catch((error) => expect(error).toEqual(new Error('Invalid time range type: invalid')));
    });

    it('does not do anything if type stays the same', () => QueriesActions.rangeType('query1', 'relative')
      .then((newQueries) => expect(newQueries).toEqual(Immutable.OrderedMap<QueryId, Query>({ query1 }))));

    it('translates current relative time range parameters to absolute ones when switching to absolute',
      () => QueriesActions.rangeType('query1', 'absolute')
        .then((newQueries) => expect(newQueries)
          .toEqual(Immutable.OrderedMap<QueryId, Query>({
            query1: query1.toBuilder()
              .timerange({
                type: 'absolute',
                from: moment(fixedDate).subtract(300, 'seconds').toISOString(),
                to: moment(fixedDate).toISOString(),
              })
              .build(),
          }))));

    it('translates current relative time range parameters to keyword when switching to keyword',
      () => QueriesActions.rangeType('query1', 'keyword')
        .then((newQueries) => expect(newQueries)
          .toEqual(Immutable.OrderedMap<QueryId, Query>({
            query1: query1.toBuilder()
              .timerange({
                type: 'keyword',
                keyword: 'Last five Minutes',
              })
              .build(),
          }))));
  });
});
