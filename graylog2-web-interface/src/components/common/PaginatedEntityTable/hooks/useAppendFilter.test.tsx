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
import type * as Immutable from 'immutable';

import useAppendFilter from 'components/common/PaginatedEntityTable/hooks/useAppendFilter';

const mockSetUrlFilters = jest.fn();

jest.mock('components/common/EntityFilters/hooks/useUrlQueryFilters', () => {
  const immutable = jest.requireActual<typeof Immutable>('immutable');

  return {
    __esModule: true,
    default: () => [immutable.OrderedMap<string, Array<string>>({ tags: ['phishing'] }), mockSetUrlFilters],
  };
});

describe('useAppendFilter', () => {
  beforeEach(() => {
    mockSetUrlFilters.mockReset();
  });

  it('appends to the given attribute without touching other filters', () => {
    const { result } = renderHook(() => useAppendFilter('tactics_techniques'));

    result.current('T1059');

    expect(mockSetUrlFilters).toHaveBeenCalledTimes(1);
    const next = mockSetUrlFilters.mock.calls[0][0];
    expect(next.get('tactics_techniques')).toEqual(['T1059']);
    expect(next.get('tags')).toEqual(['phishing']);
  });
});
