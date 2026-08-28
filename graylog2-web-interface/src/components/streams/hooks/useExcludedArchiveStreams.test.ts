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
import { PluginStore } from 'graylog-web-plugin/plugin';

import { asMock } from 'helpers/mocking';

import useExcludedArchiveStreams from './useExcludedArchiveStreams';

jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: { exports: jest.fn(() => []) },
}));

describe('useExcludedArchiveStreams', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('returns an empty list when the archive plugin is not installed', () => {
    asMock(PluginStore.exports).mockReturnValue([]);

    const { result } = renderHook(() => useExcludedArchiveStreams());

    expect(result.current).toEqual([]);
  });

  it('returns the excluded streams provided by the archive plugin', () => {
    asMock(PluginStore.exports).mockReturnValue([{ hooks: { useExcludedStreams: () => ['stream-1', 'stream-2'] } }]);

    const { result } = renderHook(() => useExcludedArchiveStreams());

    expect(result.current).toEqual(['stream-1', 'stream-2']);
  });
});
