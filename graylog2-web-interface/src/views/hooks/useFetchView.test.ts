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
import { waitFor } from 'wrappedTestingLibrary';
import { renderHook } from 'wrappedTestingLibrary/hooks';
import { List } from 'immutable';

import type { ViewJson } from 'views/logic/views/View';
import { asMock } from 'helpers/mocking';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import Search from 'views/logic/search/Search';
import ViewDeserializer from 'views/logic/views/ViewDeserializer';
import View from 'views/logic/views/View';

import useFetchView from './useFetchView';

const viewJson: ViewJson = {
  id: 'foo',
  type: 'DASHBOARD',
  title: 'Foo',
  summary: 'summary',
  description: 'Foo',
  search_id: 'foosearch',
  properties: List<any>(),
  state: {},
  created_at: '2022-01-01 00:00:00',
  last_updated_at: '2022-01-01 00:00:00',
  owner: 'admin',
  requires: {},
  favorite: false,
};

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
