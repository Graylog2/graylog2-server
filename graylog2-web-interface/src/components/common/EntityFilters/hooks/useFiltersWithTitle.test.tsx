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
import { renderHook } from 'wrappedTestingLibrary/hooks';
import { OrderedMap } from 'immutable';
import { defaultTimezone } from 'defaultMockValues';
import * as React from 'react';

import { asMock } from 'helpers/mocking';
import fetch from 'logic/rest/FetchProvider';
import { attributes } from 'fixtures/entityListAttributes';
import UserDateTimeProvider from 'contexts/UserDateTimeProvider';

import useFiltersWithTitle from './useFiltersWithTitle';

jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve({
  entities: [],
  not_allowed_entities: [],
})));

describe('useFiltersWithTitle', () => {
  const urlQueryFilters = OrderedMap({
    index_set_id: ['index_set_id_1', 'index_set_id_2'],
    created_at: ['2023-03-23T13:42:50.733+00:00><'],
    disabled: ['false'],
  });

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <UserDateTimeProvider tz={defaultTimezone}>
      {children}
    </UserDateTimeProvider>
  );

  it('fetches titles only for filters related to attributes which have a related collection', async () => {
    const { waitFor, result } = renderHook(() => useFiltersWithTitle(urlQueryFilters, attributes), { wrapper });

    await waitFor(() => expect(result.current.isInitialLoading).toBe(true));

    await waitFor(() => expect(result.current.isInitialLoading).toBe(false));

    await waitFor(() => expect(fetch).toHaveBeenCalledWith(
      'POST',
      'http://localhost/system/catalog/entities/titles',
      { entities: [{ id: 'index_set_id_1', type: 'index_sets' }, { id: 'index_set_id_2', type: 'index_sets' }] },
    ));
  });

  it('generates correct filter names for all attribute types', async () => {
    asMock(fetch).mockReturnValue(Promise.resolve({ entities: [{ id: 'index_set_id_1', type: 'index_sets', title: 'Index set 1' }], not_permitted_to_view: ['index_set_id_2'] }));
    const { waitFor, result } = renderHook(() => useFiltersWithTitle(urlQueryFilters, attributes), { wrapper });

    await waitFor(() => expect(result.current.data).toEqual(OrderedMap({
      index_set_id: [
        { title: 'Index set 1', value: 'index_set_id_1' },
        { title: 'index_set_id_2', value: 'index_set_id_2' },
      ],
      created_at: [
        {
          title: '2023-03-23 14:42:50 - Now',
          value: '2023-03-23T13:42:50.733+00:00><',
        },
      ],
      disabled: [{ title: 'Running', value: 'false' }],
    })));
  });
});
