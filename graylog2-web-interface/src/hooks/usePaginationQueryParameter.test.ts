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
import { MemoryRouter as Router } from 'react-router-dom';
import type { Location } from 'history';

import useLocation from 'routing/useLocation';
import { asMock } from 'helpers/mocking';

import usePaginationQueryParameter, { DEFAULT_PAGE } from './usePaginationQueryParameter';

const DEFAULT_PAGE_SIZES = [10, 50, 100];
const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

jest.mock('routing/useLocation', () => jest.fn(() => ({ pathname: '', search: '' })));

const options = { wrapper: Router };

describe('usePaginationQueryParameter custom hook', () => {
  it('should use default pagination if there is no <page> query parameter', () => {
    const { result } = renderHook(() => usePaginationQueryParameter(DEFAULT_PAGE_SIZES), options);

    const { page } = result.current;

    expect(page).toEqual(DEFAULT_PAGE);
  });

  it('should use default pageSize if there is no <pageSize> query parameter', () => {
    const { result } = renderHook(() => usePaginationQueryParameter(DEFAULT_PAGE_SIZES), options);

    const { pageSize } = result.current;

    expect(pageSize).toEqual(DEFAULT_PAGE_SIZES[0]);
  });

  it('should set <page> query parameter with the value sent in setPagination', () => {
    const { result } = renderHook(() => usePaginationQueryParameter(DEFAULT_PAGE_SIZES), options);

    const { page, setPagination } = result.current;

    expect(page).toEqual(DEFAULT_PAGE);

    const nextPage = 4;

    act(() => setPagination({ page: nextPage }));

    expect(mockNavigate).toHaveBeenCalledWith(`?page=${nextPage}&pageSize=10`);
  });

  it('should set <pageSize> query parameter with the value sent in setPagination and initialize the <page> query parameter', () => {
    const { result } = renderHook(() => usePaginationQueryParameter(DEFAULT_PAGE_SIZES), options);

    const { pageSize, setPagination } = result.current;

    expect(pageSize).toEqual(DEFAULT_PAGE_SIZES[0]);

    const nextPageSize = DEFAULT_PAGE_SIZES[1];

    act(() => setPagination({ pageSize: nextPageSize }));

    expect(mockNavigate).toHaveBeenCalledWith(`?page=${DEFAULT_PAGE}&pageSize=${nextPageSize}`);
  });

  it('should get the page value from <page> query parameter', () => {
    const currentPage = 7;

    asMock(useLocation).mockReturnValue({
      search: `?page=${currentPage}`,
    } as Location);

    const { result } = renderHook(() => usePaginationQueryParameter(DEFAULT_PAGE_SIZES), options);

    const { page } = result.current;

    expect(page).toEqual(currentPage);
  });

  it('should get the pageSize value from <pageSize> query parameter', () => {
    const currentPageSize = DEFAULT_PAGE_SIZES[2];

    asMock(useLocation).mockReturnValue({
      search: `?pageSize=${currentPageSize}`,
    } as Location);

    const { result } = renderHook(() => usePaginationQueryParameter(DEFAULT_PAGE_SIZES), options);

    const { pageSize } = result.current;

    expect(pageSize).toEqual(currentPageSize);
  });

  it('should only accept <pageSize> query parameter if the value is in the DEFAULT_PAGE_SIZES', () => {
    const currentPageSize = 999;

    asMock(useLocation).mockReturnValue({
      search: `?pageSize=${currentPageSize}`,
    } as Location);

    const { result } = renderHook(() => usePaginationQueryParameter(DEFAULT_PAGE_SIZES), options);

    const { pageSize } = result.current;

    expect(pageSize).not.toEqual(currentPageSize);
    expect(pageSize).toEqual(DEFAULT_PAGE_SIZES[0]);
  });

  it('should reset current page', () => {
    const currentPage = 3;
    const currentPageSize = 50;

    asMock(useLocation).mockReturnValue({
      search: `?page=${currentPage}&pageSize=${currentPageSize}`,
      pathname: 'example.org',
    } as Location);

    const { result } = renderHook(() => usePaginationQueryParameter());

    expect(result.current.page).toEqual(currentPage);
    expect(result.current.pageSize).toEqual(currentPageSize);

    result.current.resetPage();

    expect(mockNavigate).toHaveBeenCalledWith(`example.org?page=1&pageSize=${currentPageSize}`);
  });

  it('should always use provided page size and not update pageSize query param, when syncPageSizeFromQuery is false', () => {
    const queryParamsPage = 3;
    const providedPageSize = 20;

    asMock(useLocation).mockReturnValue({
      search: `?page=${queryParamsPage}&pageSize=50`,
      pathname: 'example.org',
    } as Location);

    const { result } = renderHook(() => usePaginationQueryParameter(undefined, providedPageSize, false));

    expect(result.current.page).toEqual(queryParamsPage);
    expect(result.current.pageSize).toEqual(providedPageSize);

    result.current.setPagination({ page: 4, pageSize: 100 });

    expect(mockNavigate).toHaveBeenCalledWith('example.org?page=4');
  });
});
