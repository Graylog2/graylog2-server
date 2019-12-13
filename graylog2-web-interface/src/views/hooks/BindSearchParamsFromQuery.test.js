import MockQuery from 'views/logic/queries/Query';
import { StoreMock as MockStore } from 'helpers/mocking';
import { QueriesActions } from 'views/stores/QueriesStore';
import bindSearchParamsFromQuery from './BindSearchParamsFromQuery';

jest.mock('views/stores/QueriesStore', () => ({
  QueriesActions: {
    query: jest.fn(() => Promise.resolve()),
    timerange: jest.fn(() => Promise.resolve()),
  },
}));

jest.mock('views/stores/CurrentQueryStore', () => ({
  CurrentQueryStore: MockStore(['getInitialState', () => MockQuery.builder().id('query-id').build()], 'listen'),
}));

describe('BindSearchParamsFromQuery should', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('not update query when provided view is not a search', async () => {
    await bindSearchParamsFromQuery({ query: undefined, view: { type: 'DASHBOARD' } });
    expect(QueriesActions.query).not.toHaveBeenCalled();
  });

  it('update query string with provided query param', async () => {
    await bindSearchParamsFromQuery({ query: { q: 'gl2_source_input:source-input-id' }, view: { type: 'SEARCH' } });
    expect(QueriesActions.query).toHaveBeenCalledWith('query-id', 'gl2_source_input:source-input-id');
  });

  it('not update query string when no query param is provided', async () => {
    await bindSearchParamsFromQuery({ query: undefined, view: { type: 'SEARCH' } });
    expect(QueriesActions.query).not.toHaveBeenCalled();
  });

  it('update query timerange when relative range value param is povided', async () => {
    await bindSearchParamsFromQuery({ query: { relative: '0' }, view: { type: 'SEARCH' } });
    expect(QueriesActions.timerange).toHaveBeenCalledWith('query-id', { type: 'relative', range: '0' });
  });

  it('update query timerange when provided query range param is absolute', async () => {
    await bindSearchParamsFromQuery({ query: { rangetype: 'absolute', from: '2010-01-00 00:00:00', to: '2010-10-00 00:00:00' }, view: { type: 'SEARCH' } });
    expect(QueriesActions.timerange).toHaveBeenCalledWith('query-id', { type: 'absolute', from: '2010-01-00 00:00:00', to: '2010-10-00 00:00:00' });
  });

  it('update query timerange when provided query range is keyword', async () => {
    await bindSearchParamsFromQuery({ query: { rangetype: 'keyword', keyword: 'Last five days' }, view: { type: 'SEARCH' } });
    expect(QueriesActions.timerange).toHaveBeenCalledWith('query-id', { type: 'keyword', keyword: 'Last five days' });
  });
});
