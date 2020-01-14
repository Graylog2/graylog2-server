// @flow strict
import moment from 'moment';
import * as Immutable from 'immutable';

import asMock from 'helpers/mocking/AsMock';
import { QueriesActions, QueriesStore } from './QueriesStore';
import type { QueryId } from '../logic/queries/Query';
import Query from '../logic/queries/Query';
import { ViewStore } from './ViewStore';
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
    const callback = asMock(ViewStore).listen.mock.calls[0][0];
    const unsubscribe = QueriesStore.listen((newQueries) => {
      expect(newQueries).toEqual(new Immutable.OrderedMap<QueryId, Query>({ query1 }));
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
      const callback = asMock(ViewStore).listen.mock.calls[0][0];

      callback({
        view: {
          search: Search.builder().queries(queries).build(),
        },
      });
    });
    it('throws error if no type is given', () => {
      expect(
        // $FlowFixMe: Passing no second argument on purpose
        QueriesActions.rangeType('query1', undefined),
      ).rejects.toEqual(new Error('Invalid time range type: undefined'));
    });
    it('throws error if invalid type is given', () => {
      expect(
        // $FlowFixMe: Passing invalid second argument on purpose
        QueriesActions.rangeType('query1', 'invalid'),
      ).rejects.toEqual(new Error('Invalid time range type: invalid'));
    });
    it('does not do anything if type stays the same', () => {
      expect(QueriesActions.rangeType('query1', 'relative')).resolves.toEqual(new Immutable.OrderedMap<QueryId, Query>({ query1 }));
    });
    it('translates current relative time range parameters to absolute ones when switching to absolute', () => {
      expect(QueriesActions.rangeType('query1', 'absolute')).resolves.toEqual(new Immutable.OrderedMap<QueryId, Query>({
        query1: query1.toBuilder()
          .timerange({
            type: 'absolute',
            from: moment().subtract(300, 'seconds').toISOString(),
            to: moment().toISOString(),
          })
          .build(),
      }));
    });
    it('translates current relative time range parameters to keyword when switching to keyword', () => {
      expect(QueriesActions.rangeType('query1', 'keyword')).resolves.toEqual(new Immutable.OrderedMap<QueryId, Query>({
        query1: query1.toBuilder()
          .timerange({
            type: 'keyword',
            keyword: 'Last five minutes',
          })
          .build(),
      }));
    });
  });
});
