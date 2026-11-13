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
import { renderHook, waitFor } from 'wrappedTestingLibrary/hooks';

import { QuickJump } from '@graylog/server-api';

import { usePluginExports } from 'views/test/testPlugins';
import asMock from 'helpers/mocking/AsMock';
import useNavItems from 'components/quick-jump/hooks/useNavItems';
import { ACTION_TYPE, PAGE_TYPE } from 'components/quick-jump/Constants';
import type { QualifiedUrl } from 'routing/Routes';

import useQuickJumpSearch from './useQuickJumpSearch';

jest.mock('@graylog/server-api', () => ({
  QuickJump: { search: jest.fn() },
}));
jest.mock('components/quick-jump/hooks/useNavItems');

describe('useQuickJumpSearch', () => {
  usePluginExports({
    'quickJump.queryActions': [
      {
        key: 'ask',
        item: (query: string) => ({ type: ACTION_TYPE, title: `Ask about "${query}"`, link: `/ask?q=${query}` as QualifiedUrl<string> }),
      },
      { key: 'hidden', item: () => ({ type: ACTION_TYPE, title: 'Hidden', link: '/hidden' as QualifiedUrl<string> }), useCondition: () => false },
    ],
  });

  beforeEach(() => {
    asMock(QuickJump.search).mockResolvedValue({ results: [] } as Awaited<ReturnType<typeof QuickJump.search>>);
    asMock(useNavItems).mockReturnValue([{ type: PAGE_TYPE, title: 'Streams', link: '/streams' as QualifiedUrl<string> }]);
  });

  it('offers actions for the typed text after everything that matches it', async () => {
    const { result } = renderHook(() => useQuickJumpSearch(' streams '));

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.searchResults.map(({ title }) => title)).toEqual(['Streams', 'Ask about "streams"']);
    expect(result.current.searchResults[1]).toEqual(
      expect.objectContaining({ key: 'ask', link: '/ask?q=streams' }),
    );
  });

  it('offers no actions without text', async () => {
    const { result } = renderHook(() => useQuickJumpSearch(''));

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.searchResults).toEqual([]);
  });
});
