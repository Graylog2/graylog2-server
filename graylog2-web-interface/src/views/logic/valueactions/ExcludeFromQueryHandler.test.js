// @flow strict
import * as Immutable from 'immutable';

import { QueriesActions, QueriesStore } from 'views/stores/QueriesStore';
import Query from '../queries/Query';

import ExcludeFromQueryHandler from './ExcludeFromQueryHandler';

jest.mock('views/stores/QueriesStore', () => ({
  QueriesStore: {
    getInitialState: jest.fn(),
    listen: jest.fn(),
  },
  QueriesActions: {
    query: jest.fn(),
  },
}));

const queryWithQueryString = queryString => Query.builder().query({ type: 'elasticsearch', query_string: queryString }).build();

const mockQueries = (queryId: string, queryString: string) => Immutable.Map({ [queryId]: queryWithQueryString(queryString) });

describe('ExcludeFromQueryHandler', () => {
  afterEach(() => {
    jest.resetAllMocks();
  });

  it('adds exclusion term to query', () => {
    QueriesStore.getInitialState.mockReturnValue(mockQueries('queryId', ''));

    const handler = new ExcludeFromQueryHandler();
    handler.handle({ queryId: 'queryId', field: 'something', value: 'other' });

    expect(QueriesActions.query).toHaveBeenCalledWith('queryId', 'NOT something:other');
  });

  it('replaces `*` query completely', () => {
    QueriesStore.getInitialState.mockReturnValue(mockQueries('queryId', '*'));

    const handler = new ExcludeFromQueryHandler();
    handler.handle({ queryId: 'queryId', field: 'foo', value: 'bar' });

    expect(QueriesActions.query).toHaveBeenCalledWith('queryId', 'NOT foo:bar');
  });

  it('appends negated term to existing query', () => {
    QueriesStore.getInitialState.mockReturnValue(mockQueries('queryId', 'answer:42'));

    const handler = new ExcludeFromQueryHandler();
    handler.handle({ queryId: 'queryId', field: 'do', value: 'panic' });

    expect(QueriesActions.query).toHaveBeenCalledWith('queryId', 'answer:42 AND NOT do:panic');
  });

  it('escapes special characters in field value', () => {
    QueriesStore.getInitialState.mockReturnValue(mockQueries('queryId', '*'));

    const handler = new ExcludeFromQueryHandler();
    handler.handle({ queryId: 'queryId', field: 'something', value: 'foo && || : \\ / + - ! ( ) { } [ ] ^ " ~ * ? bar' });

    expect(QueriesActions.query).toHaveBeenCalledWith('queryId', 'NOT something:"foo && || : \\\\ / + - ! ( ) { } [ ] ^ \\" ~ * ? bar"');
  });
});
