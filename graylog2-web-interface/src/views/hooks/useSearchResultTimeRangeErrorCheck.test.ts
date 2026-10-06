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
import useCurrentQuery from 'views/logic/queries/useCurrentQuery';
import useSearchResult from 'views/hooks/useSearchResult';
import useIsLoading from 'views/hooks/useIsLoading';
import type Query from 'views/logic/queries/Query';
import type { TimeRange } from 'views/logic/queries/Query';
import type { SearchExecutionResult } from 'views/types';

import useSearchResultTimeRangeErrorCheck from './useSearchResultTimeRangeErrorCheck';

jest.mock('views/logic/queries/useCurrentQuery');
jest.mock('views/hooks/useSearchResult');
jest.mock('views/hooks/useIsLoading');

const errorType = 'query_time_range_limit';
const timerange: TimeRange = { type: 'relative', from: 300 };
const currentQuery = { id: 'query-1', timerange } as Query;

const mockSearchResult = (errors: Array<{ queryId: string; type: string }> = []) =>
  ({
    result: {
      errors,
    },
  }) as SearchExecutionResult;

const renderSubject = () => renderHook(() => useSearchResultTimeRangeErrorCheck(errorType));

describe('useSearchResultTimeRangeErrorCheck', () => {
  beforeEach(() => {
    asMock(useCurrentQuery).mockReturnValue(currentQuery);
    asMock(useIsLoading).mockReturnValue(false);
    asMock(useSearchResult).mockReturnValue(mockSearchResult());
  });

  it('returns true when matching query has the requested time range error and timerange matches', () => {
    asMock(useSearchResult).mockReturnValue(
      mockSearchResult([
        {
          queryId: 'query-1',
          type: errorType,
        },
      ]),
    );

    const { result } = renderSubject();

    expect(result.current(timerange)).toBe(true);
  });

  it('returns false when current timerange does not match executed timerange', () => {
    asMock(useSearchResult).mockReturnValue(
      mockSearchResult([
        {
          queryId: 'query-1',
          type: errorType,
        },
      ]),
    );

    const { result } = renderSubject();

    expect(result.current({ type: 'relative', from: 60 })).toBe(false);
  });

  it('returns false when execution is loading', () => {
    asMock(useSearchResult).mockReturnValue(
      mockSearchResult([
        {
          queryId: 'query-1',
          type: errorType,
        },
      ]),
    );
    asMock(useIsLoading).mockReturnValue(true);

    const { result } = renderSubject();

    expect(result.current(timerange)).toBe(false);
  });

  it('returns false when only different error types are present', () => {
    asMock(useSearchResult).mockReturnValue(
      mockSearchResult([
        {
          queryId: 'query-1',
          type: 'search_type_time_range_limit',
        },
      ]),
    );

    const { result } = renderSubject();

    expect(result.current(timerange)).toBe(false);
  });

  it('returns false when the error belongs to another query', () => {
    asMock(useSearchResult).mockReturnValue(
      mockSearchResult([
        {
          queryId: 'query-2',
          type: errorType,
        },
      ]),
    );

    const { result } = renderSubject();

    expect(result.current(timerange)).toBe(false);
  });
});
