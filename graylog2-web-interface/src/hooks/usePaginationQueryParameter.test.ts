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
import { renderHook, act } from 'wrappedTestingLibrary/hooks';

import { useQueryParams } from 'routing/QueryParams';
import { asMock } from 'helpers/mocking';

import usePaginationQueryParameter, { DEFAULT_PAGE } from './usePaginationQueryParameter';

const DEFAULT_PAGE_SIZES = [10, 50, 100];
const setQueryParams = jest.fn();
let queryParams: { page?: string | number; pageSize?: string | number; filters?: string };

jest.mock('routing/QueryParams', () => ({
  ...jest.requireActual('routing/QueryParams'),
  useQueryParams: jest.fn(),
}));

describe('usePaginationQueryParameter custom hook', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    queryParams = {};
    asMock(useQueryParams).mockImplementation(() => [queryParams, setQueryParams]);
  });

  it('should use default pagination if there is no <page> query parameter', () => {
    const { result } = renderHook(() => usePaginationQueryParameter(DEFAULT_PAGE_SIZES));

    expect(result.current.page).toEqual(DEFAULT_PAGE);
  });

  it('should use default pageSize if there is no <pageSize> query parameter', () => {
    const { result } = renderHook(() => usePaginationQueryParameter(DEFAULT_PAGE_SIZES));

    expect(result.current.pageSize).toEqual(DEFAULT_PAGE_SIZES[0]);
  });

  it('should set <page> query parameter with the value sent in setPagination', () => {
    const { result } = renderHook(() => usePaginationQueryParameter(DEFAULT_PAGE_SIZES));

    act(() => result.current.setPagination({ page: 4 }));

    expect(setQueryParams).toHaveBeenCalledWith({ page: 4, pageSize: 10 });
  });

  it('should set <pageSize> query parameter with the value sent in setPagination and initialize the <page> query parameter', () => {
    const { result } = renderHook(() => usePaginationQueryParameter(DEFAULT_PAGE_SIZES));

    act(() => result.current.setPagination({ pageSize: DEFAULT_PAGE_SIZES[1] }));

    expect(setQueryParams).toHaveBeenCalledWith({ page: DEFAULT_PAGE, pageSize: DEFAULT_PAGE_SIZES[1] });
  });

  it('should get the page value from <page> query parameter', () => {
    const currentPage = 7;
    queryParams = { page: currentPage };

    const { result } = renderHook(() => usePaginationQueryParameter(DEFAULT_PAGE_SIZES));

    expect(result.current.page).toEqual(currentPage);
  });

  it('should get the pageSize value from <pageSize> query parameter', () => {
    const currentPageSize = DEFAULT_PAGE_SIZES[2];
    queryParams = { pageSize: currentPageSize };

    const { result } = renderHook(() => usePaginationQueryParameter(DEFAULT_PAGE_SIZES));

    expect(result.current.pageSize).toEqual(currentPageSize);
  });

  it('should only accept <pageSize> query parameter if the value is in the DEFAULT_PAGE_SIZES', () => {
    const currentPageSize = 999;
    queryParams = { pageSize: currentPageSize };

    const { result } = renderHook(() => usePaginationQueryParameter(DEFAULT_PAGE_SIZES));

    expect(result.current.pageSize).toEqual(DEFAULT_PAGE_SIZES[0]);
  });

  it('should reset current page', () => {
    queryParams = { page: 3, pageSize: 50 };
    const { result } = renderHook(() => usePaginationQueryParameter(DEFAULT_PAGE_SIZES));

    act(() => result.current.resetPage());

    expect(setQueryParams).toHaveBeenCalledWith({ page: DEFAULT_PAGE, pageSize: 50 });
  });

  it('should always use provided page size and not update pageSize query param, when syncPageSizeFromQuery is false', () => {
    queryParams = { page: 3, pageSize: 50 };
    const providedPageSize = 20;
    const { result } = renderHook(() => usePaginationQueryParameter(undefined, providedPageSize, false));

    act(() => result.current.setPagination({ page: 4, pageSize: 100 }));

    expect(setQueryParams).toHaveBeenCalledWith({ page: 4, pageSize: undefined });
  });

  it('should update page without replacing unrelated query params like filters', () => {
    queryParams = { page: 2, filters: 'owner=local:admin' };
    const { result } = renderHook(() => usePaginationQueryParameter(DEFAULT_PAGE_SIZES));

    act(() => result.current.setPagination({ page: 1 }));

    expect(setQueryParams).toHaveBeenCalledWith({ page: 1, pageSize: 10 });
  });
});
