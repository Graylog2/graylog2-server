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
import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';

import { useQueryParam } from 'routing/QueryParams';
import DefaultQueryParamProvider from 'routing/DefaultQueryParamProvider';
import { asMock } from 'helpers/mocking';

import useUrlQueryFilters from './useUrlQueryFilters';

jest.mock('routing/QueryParams', () => ({
  ...jest.requireActual('routing/QueryParams'),
  useQueryParam: jest.fn(),
}));

describe('useUrlQueryFilters', () => {
  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <MemoryRouter>
      <DefaultQueryParamProvider>{children}</DefaultQueryParamProvider>
    </MemoryRouter>
  );

  beforeEach(() => {
    asMock(useQueryParam).mockReturnValue([['index_set_id=index_set_id_1', 'index_set_id=index_set_id_2'], () => {}]);
  });

  it('provides correct response for URL query params', async () => {
    const { waitFor, result } = renderHook(() => useUrlQueryFilters(), { wrapper });

    await waitFor(() =>
      expect(result.current[0]).toEqual(OrderedMap({ index_set_id: ['index_set_id_1', 'index_set_id_2'] })),
    );
  });

  it('updates URL query params correctly', async () => {
    const updateUrlQueryParams = jest.fn();
    asMock(useQueryParam).mockReturnValue([[], updateUrlQueryParams]);
    const { waitFor, result } = renderHook(() => useUrlQueryFilters(), { wrapper });

    result.current[1](OrderedMap({ index_set_id: ['index_set_id_1', 'index_set_id_2'] }));

    await waitFor(() =>
      expect(updateUrlQueryParams).toHaveBeenCalledWith(['index_set_id=index_set_id_1', 'index_set_id=index_set_id_2']),
    );
  });
});
