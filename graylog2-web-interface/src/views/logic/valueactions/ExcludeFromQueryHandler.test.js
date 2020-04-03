// @flow strict
import * as Immutable from 'immutable';

import asMock from 'helpers/mocking/AsMock';
import mockAction from 'helpers/mocking/MockAction';
import { GlobalOverrideActions, GlobalOverrideStore } from 'views/stores/GlobalOverrideStore';
import { QueriesActions, QueriesStore } from 'views/stores/QueriesStore';
import SearchActions from 'views/actions/SearchActions';
import { ViewStore } from 'views/stores/ViewStore';

import FieldType from '../fieldtypes/FieldType';
import GlobalOverride from '../search/GlobalOverride';
import Query from '../queries/Query';
import View from '../views/View';
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
jest.mock('views/stores/ViewStore', () => ({ ViewStore: {} }));
jest.mock('views/stores/GlobalOverrideStore', () => ({ GlobalOverrideStore: {}, GlobalOverrideActions: {} }));
jest.mock('views/actions/SearchActions', () => ({}));

const queryWithQueryString = (queryString) => Query.builder().query({ type: 'elasticsearch', query_string: queryString }).build();

const mockQueries = (queryId: string, queryString: string) => Immutable.Map({ [queryId]: queryWithQueryString(queryString) });

describe('ExcludeFromQueryHandler', () => {
  const view = View.create().toBuilder().type(View.Type.Search).build();
  afterEach(() => {
    jest.resetAllMocks();
  });
  beforeEach(() => {
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

  it('adds exclusion term to query', () => {
    asMock(QueriesStore.getInitialState).mockReturnValue(mockQueries('queryId', ''));

    const handler = new ExcludeFromQueryHandler();
    handler.handle({ queryId: 'queryId', field: 'something', value: 'other', type: FieldType.Unknown, contexts: {} });

    expect(QueriesActions.query).toHaveBeenCalledWith('queryId', 'NOT something:other');
  });

  it('replaces `*` query completely', () => {
    asMock(QueriesStore.getInitialState).mockReturnValue(mockQueries('queryId', '*'));

    const handler = new ExcludeFromQueryHandler();
    handler.handle({ queryId: 'queryId', field: 'foo', value: 'bar', type: FieldType.Unknown, contexts: {} });

    expect(QueriesActions.query).toHaveBeenCalledWith('queryId', 'NOT foo:bar');
  });

  it('appends negated term to existing query', () => {
    asMock(QueriesStore.getInitialState).mockReturnValue(mockQueries('queryId', 'answer:42'));

    const handler = new ExcludeFromQueryHandler();
    handler.handle({ queryId: 'queryId', field: 'do', value: 'panic', type: FieldType.Unknown, contexts: {} });

    expect(QueriesActions.query).toHaveBeenCalledWith('queryId', 'answer:42 AND NOT do:panic');
  });

  it('escapes special characters in field value', () => {
    asMock(QueriesStore.getInitialState).mockReturnValue(mockQueries('queryId', '*'));

    const handler = new ExcludeFromQueryHandler();
    handler.handle({ queryId: 'queryId', field: 'something', value: 'foo && || : \\ / + - ! ( ) { } [ ] ^ " ~ * ? bar', type: FieldType.Unknown, contexts: {} });

    expect(QueriesActions.query).toHaveBeenCalledWith('queryId', 'NOT something:"foo && || : \\\\ / + - ! ( ) { } [ ] ^ \\" ~ * ? bar"');
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
      const handler = new ExcludeFromQueryHandler();
      return handler.handle({ queryId: 'queryId', field: 'do', value: 'panic', type: FieldType.Unknown, contexts: {} })
        .then(() => {
          expect(GlobalOverrideActions.query).toHaveBeenCalledWith('something AND NOT do:panic');
          expect(SearchActions.refresh).toHaveBeenCalled();
        });
    });
  });
});
