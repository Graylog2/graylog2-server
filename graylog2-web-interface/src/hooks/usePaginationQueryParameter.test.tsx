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
import type { Location } from 'history';

import { asMock } from 'helpers/mocking';

import usePaginationQueryParameter from './usePaginationQueryParameter';

const DEFAULT_PAGE_SIZES = [10, 50, 100];
const mockHistoryReplace = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useHistory: () => ({
    replace: mockHistoryReplace,
  }),
  useLocation: jest.fn(() => ({
    pathname: '',
    search: '',
  })),
}));

describe('usePaginationQueryParameter custom hook', () => {
  it('should use default pagination if there is no <page> query parameter', () => {
    const { result } = renderHook(() => usePaginationQueryParameter(DEFAULT_PAGE_SIZES));

    const { page } = result.current;

    expect(page).toEqual(1);
  });

  it('should set <page> query parameter with the value sent in setPage', () => {
    const { result } = renderHook(() => usePaginationQueryParameter(DEFAULT_PAGE_SIZES));

    const { page, setPage } = result.current;

    expect(page).toEqual(1);

    const nextPage = 4;

    act(() => setPage(nextPage));

    expect(mockHistoryReplace).toHaveBeenCalledWith(`?page=${nextPage}`);
  });

  it('should get the page value from <page> query parameter', () => {
    const currentPage = 7;

    asMock(useLocation).mockReturnValue({
      search: `?page=${currentPage}`,
    } as Location<{ search: string }>);

    const { result } = renderHook(() => usePaginationQueryParameter(DEFAULT_PAGE_SIZES));

    const { page } = result.current;

    expect(page).toEqual(currentPage);
  });
});
