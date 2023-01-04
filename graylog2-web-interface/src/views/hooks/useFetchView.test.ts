import { renderHook } from '@testing-library/react-hooks';
import { List } from 'immutable';
import { waitFor } from 'wrappedTestingLibrary';

import type { ViewJson } from 'views/logic/views/View';
import { asMock } from 'helpers/mocking';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import Search from 'views/logic/search/Search';
import ViewDeserializer from 'views/logic/views/ViewDeserializer';
import View from 'views/logic/views/View';

import useFetchView from './useFetchView';

const viewJson = {
  id: 'foo',
  type: 'DASHBOARD',
  title: 'Foo',
  summary: 'summary',
  description: 'Foo',
  search_id: 'foosearch',
  properties: List<any>(),
  state: {},
  dashboard_state: { widgets: [], positions: [] },
  created_at: '2022-01-01 00:00:00',
  owner: 'admin',
  requires: {},
} as ViewJson;

jest.mock('views/stores/ViewManagementStore', () => ({
  ViewManagementActions: {
    get: jest.fn(),
    update: {
      completed: {
        listen: jest.fn(),
      },
    },
  },
}));

jest.mock('views/logic/views/ViewDeserializer');

jest.mock('actions/errors/ErrorsActions', () => ({
  report: jest.fn(),
}));

describe('useFetchView', () => {
  beforeEach(() => {
    asMock(ViewManagementActions.get).mockResolvedValue(viewJson);
    const search = Search.create().toBuilder().parameters([]).build();

    asMock(ViewDeserializer).mockImplementation(async (response: ViewJson) => View.fromJSON(response).toBuilder().search(search).build());
  });

  it('fetches view', () => {
    renderHook(() => useFetchView('foo'));

    expect(ViewManagementActions.get).toHaveBeenCalledWith('foo');
  });

  it('does not fetch view twice', () => {
    const { rerender } = renderHook(() => useFetchView('foo'));

    expect(ViewManagementActions.get).toHaveBeenCalledWith('foo');

    asMock(ViewManagementActions.get).mockClear();

    rerender();

    expect(ViewManagementActions.get).not.toHaveBeenCalled();
  });

  it('does fetch view twice when id changes', () => {
    const { rerender } = renderHook(({ id }) => useFetchView(id), { initialProps: { id: 'foo' } });

    expect(ViewManagementActions.get).toHaveBeenCalledWith('foo');

    asMock(ViewManagementActions.get).mockClear();

    rerender({ id: 'bar' });

    expect(ViewManagementActions.get).toHaveBeenCalledWith('bar');
  });

  it('passes loaded view to ViewDeserializer', async () => {
    renderHook(() => useFetchView('foo'));

    await waitFor(() => expect(ViewDeserializer).toHaveBeenCalledWith(viewJson));
  });
});
