// @flow strict
import View from 'views/logic/views/View';
import Query from 'views/logic/queries/Query';
import Search from 'views/logic/search/Search';
import { QueriesActions } from 'views/stores/QueriesStore';
import bindSearchParamsFromQuery from './BindSearchParamsFromQuery';

const MOCK_VIEW_QUERY_ID = 'query-id';

jest.mock('views/stores/QueriesStore', () => ({
  QueriesActions: {
    query: jest.fn(() => Promise.resolve()),
    timerange: jest.fn(() => Promise.resolve()),
  },
}));

describe('BindSearchParamsFromQuery should', () => {
  const query = Query.builder().id(MOCK_VIEW_QUERY_ID).build();
  const search = Search.create()
    .toBuilder()
    .queries([query])
    .build();
  const view = View.create()
    .toBuilder()
    .type(View.Type.Search)
    .search(search)
    .build();
  const defaultInput = {
    query: {},
    view,
    retry: () => Promise.resolve(),
  };

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('not update query when provided view is not a search', async () => {
    const input = {
      ...defaultInput,
      view: view.toBuilder().type(View.Type.Dashboard).build(),
    };
    await bindSearchParamsFromQuery(input);
    expect(QueriesActions.query).not.toHaveBeenCalled();
  });

  it('update query string with provided query param', async () => {
    const input = {
      ...defaultInput,
      query: { q: 'gl2_source_input:source-input-id' },
    };
    await bindSearchParamsFromQuery(input);
    expect(QueriesActions.query).toHaveBeenCalledWith(MOCK_VIEW_QUERY_ID, input.query.q);
  });

  it('not update query string when no query param is provided', async () => {
    await bindSearchParamsFromQuery(defaultInput);
    expect(QueriesActions.query).not.toHaveBeenCalled();
  });

  it('update query timerange when relative range value param is povided', async () => {
    const input = {
      ...defaultInput,
      query: { relative: '0' },
    };
    const expectedTimerange = {
      type: 'relative',
      range: input.query.relative,
    };
    await bindSearchParamsFromQuery(input);
    expect(QueriesActions.timerange).toHaveBeenCalledWith(MOCK_VIEW_QUERY_ID, expectedTimerange);
  });

  it('update query timerange when provided query range param is absolute', async () => {
    const input = {
      ...defaultInput,
      query: { rangetype: 'absolute', from: '2010-01-00 00:00:00', to: '2010-10-00 00:00:00' },
    };
    const expectedTimerange = {
      type: input.query.rangetype,
      from: input.query.from,
      to: input.query.to,
    };
    await bindSearchParamsFromQuery(input);
    expect(QueriesActions.timerange).toHaveBeenCalledWith(MOCK_VIEW_QUERY_ID, expectedTimerange);
  });

  it('update query timerange when provided query range is keyword', async () => {
    const input = {
      ...defaultInput,
      query: { rangetype: 'keyword', keyword: 'Last five days' },
    };
    const expectedTimerange = {
      type: input.query.rangetype, keyword: input.query.keyword,
    };
    await bindSearchParamsFromQuery(input);
    expect(QueriesActions.timerange).toHaveBeenCalledWith(MOCK_VIEW_QUERY_ID, expectedTimerange);
  });
});
