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
import { renderHook, act } from '@testing-library/react-hooks';
import { useLocation } from 'react-router-dom';
import { stringify } from 'qs';
import type { Location } from 'history';

import { asMock } from 'helpers/mocking';

import useLocationSearchPagination from './useLocationSearchPagination';

const mockHistoryPush = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useHistory: () => ({
    push: mockHistoryPush,
  }),
  useLocation: jest.fn(() => ({
    pathname: '',
    search: '',
  })),
}));

describe('useLocationSearchPagination custom hook', () => {
  const DEFAULT_PAGINATION = { page: 999, perPage: 999, query: 'foobar' };

  it('uses default pagination information when location search is empty', () => {
    const { result } = renderHook(() => useLocationSearchPagination(DEFAULT_PAGINATION));

    const { isInitialized, pagination } = result.current;

    expect(isInitialized).toEqual(true);
    expect(pagination).toEqual(DEFAULT_PAGINATION);
  });

  it('reads and writes pagination information to location search', () => {
    const currentPage = { page: 1, perPage: 10, query: 'test' };

    asMock(useLocation).mockReturnValue({
      search: stringify(currentPage),
    } as Location<{ search: string }>);

    const { result } = renderHook(() => useLocationSearchPagination(DEFAULT_PAGINATION));

    const { isInitialized, pagination, setPagination } = result.current;

    expect(isInitialized).toEqual(true);
    expect(pagination).toEqual(currentPage);

    const nextPage = { page: 2, perPage: 10, query: 'lolwut' };

    act(() => setPagination(nextPage));

    expect(mockHistoryPush).toHaveBeenCalledWith({ search: stringify(nextPage) });
  });

  it.each`
    param        | description             | value                          | expectedReturn
    ${'page'}    | ${'not a number'}       | ${'foobar'}                    | ${DEFAULT_PAGINATION.page}
    ${'page'}    | ${'not a safe integer'} | ${Number.MAX_SAFE_INTEGER + 1} | ${DEFAULT_PAGINATION.page}
    ${'page'}    | ${'not a number'}       | ${NaN}                         | ${DEFAULT_PAGINATION.page}
    ${'page'}    | ${'undefined'}          | ${undefined}                   | ${DEFAULT_PAGINATION.page}
    ${'page'}    | ${'empty string'}       | ${''}                          | ${DEFAULT_PAGINATION.page}
    ${'page'}    | ${'negative number'}    | ${-1}                          | ${DEFAULT_PAGINATION.page}
    ${'perPage'} | ${'not a number'}       | ${'foobar'}                    | ${DEFAULT_PAGINATION.perPage}
    ${'perPage'} | ${'not a safe integer'} | ${Number.MAX_SAFE_INTEGER + 1} | ${DEFAULT_PAGINATION.perPage}
    ${'perPage'} | ${'not a number'}       | ${NaN}                         | ${DEFAULT_PAGINATION.perPage}
    ${'perPage'} | ${'undefined'}          | ${undefined}                   | ${DEFAULT_PAGINATION.perPage}
    ${'perPage'} | ${'empty string'}       | ${''}                          | ${DEFAULT_PAGINATION.perPage}
    ${'perPage'} | ${'negative number'}    | ${-1}                          | ${DEFAULT_PAGINATION.perPage}
    ${'query'}   | ${'not a string'}       | ${undefined}                   | ${DEFAULT_PAGINATION.query}
  `('uses default values when $param is $description', ({ param, value, expectedReturn }) => {
    asMock(useLocation).mockReturnValue({
      search: stringify({ ...DEFAULT_PAGINATION, [param]: value }),
    } as Location<{ search: string }>);

    const { result } = renderHook(() => useLocationSearchPagination(DEFAULT_PAGINATION));

    expect(result.current.pagination).toEqual({ ...DEFAULT_PAGINATION, [param]: expectedReturn });
  });
});
