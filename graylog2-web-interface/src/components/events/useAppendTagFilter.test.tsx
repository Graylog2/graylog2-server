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
import * as React from 'react';
import { renderHook } from 'wrappedTestingLibrary/hooks';
import { OrderedMap } from 'immutable';
import type * as Immutable from 'immutable';

import useAppendTagFilter from 'components/events/useAppendTagFilter';
import TableFetchContext from 'components/common/PaginatedEntityTable/TableFetchContext';

const mockSetUrlFilters = jest.fn();

jest.mock('components/common/EntityFilters/hooks/useUrlQueryFilters', () => {
  const immutable = jest.requireActual<typeof Immutable>('immutable');

  return {
    __esModule: true,
    default: () => [immutable.OrderedMap<string, Array<string>>(), mockSetUrlFilters],
  };
});

describe('useAppendTagFilter', () => {
  beforeEach(() => {
    mockSetUrlFilters.mockReset();
  });

  it('appends a tag to the filters when outside a TableFetchContext provider', () => {
    const { result } = renderHook(() => useAppendTagFilter());

    result.current('phishing');

    expect(mockSetUrlFilters).toHaveBeenCalledTimes(1);
    const next = mockSetUrlFilters.mock.calls[0][0];
    expect(next.get('tags')).toEqual(['phishing']);
  });

  it('reads existing filters from the table context when present', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <TableFetchContext.Provider
        value={{
          searchParams: {
            page: 1,
            pageSize: 10,
            query: '',
            sort: { attributeId: 'timestamp', direction: 'desc' },
            filters: OrderedMap<string, Array<string>>({ priority: ['critical'], tags: ['exfil'] }),
          },
          refetch: () => {},
          attributes: [],
          entityTableId: 'events',
        }}>
        {children}
      </TableFetchContext.Provider>
    );

    const { result } = renderHook(() => useAppendTagFilter(), { wrapper });

    result.current('phishing');

    expect(mockSetUrlFilters).toHaveBeenCalledTimes(1);
    const next = mockSetUrlFilters.mock.calls[0][0];
    expect(next.get('priority')).toEqual(['critical']);
    expect(next.get('tags')).toEqual(['exfil', 'phishing']);
  });

  it('does not append if the tag is already in the filter list', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <TableFetchContext.Provider
        value={{
          searchParams: {
            page: 1,
            pageSize: 10,
            query: '',
            sort: { attributeId: 'timestamp', direction: 'desc' },
            filters: OrderedMap<string, Array<string>>({ tags: ['phishing'] }),
          },
          refetch: () => {},
          attributes: [],
          entityTableId: 'events',
        }}>
        {children}
      </TableFetchContext.Provider>
    );

    const { result } = renderHook(() => useAppendTagFilter(), { wrapper });

    result.current('phishing');

    expect(mockSetUrlFilters).not.toHaveBeenCalled();
  });
});
