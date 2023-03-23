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

import { asMock } from 'helpers/mocking';
import fetch from 'logic/rest/FetchProvider';
import { attributes } from 'fixtures/entityListAttributes';

import useFiltersWithTitle from './useFiltersWithTitle';

jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve({
  entities: [],
  not_allowed_entities: [],
})));

describe('useFiltersWithTitle', () => {
  const urlQueryFilters = {
    index_set_id: ['index_set_id_1'],
    created_at: ['2023-03-23T13:42:50.733+00:00><'],
    disabled: ['false'],
  };

  it('fetches titles only for filters related to attributes which have a related collection', async () => {
    const { waitFor } = renderHook(() => useFiltersWithTitle(urlQueryFilters, attributes));

    await waitFor(() => expect(fetch).toHaveBeenCalledWith(
      'POST',
      'http://localhost/system/catalog/entity_titles',
      { entities: [{ id: 'index_set_id_1', type: 'index_sets' }] },
    ));
  });

  it('generates correct filter names for all attribute types', async () => {
    asMock(fetch).mockReturnValue(Promise.resolve({ entities: [{ id: 'index_set_id_1', type: 'index_sets' }] }));
    const { waitFor, result } = renderHook(() => useFiltersWithTitle(urlQueryFilters, attributes));

    await waitFor(() => expect(result.current.data).toEqual({
      index_set_id: [
        {
          id: 'index_set_id_1',
          title: 'Loading...',
          value: 'index_set_id_1',
        },
      ],
      created_at: [
        {
          id: '2023-03-23T13:42:50.733+00:00><',
          title: '2023-03-23 14:42:50 - Now',
          value: '2023-03-23T13:42:50.733+00:00><',
        },
      ],
      disabled: [{ id: 'false', title: 'Running', value: 'false' }],
    }));
  });
});
