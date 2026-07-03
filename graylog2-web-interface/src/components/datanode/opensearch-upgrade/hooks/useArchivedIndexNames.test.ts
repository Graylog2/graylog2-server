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
import { waitFor } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';
import fetch from 'logic/rest/FetchProvider';

import useArchivedIndexNames from './useArchivedIndexNames';

jest.mock('logic/rest/FetchProvider', () => ({ __esModule: true, default: jest.fn() }));

describe('useArchivedIndexNames', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('returns the subset of index names that have a catalog entry', async () => {
    asMock(fetch).mockResolvedValue({ archives: [{ index_name: 'graylog_0' }] });

    const { result } = renderHook(() => useArchivedIndexNames(['graylog_0', 'graylog_1'], true));

    await waitFor(() => expect(result.current.has('graylog_0')).toBe(true));
    expect(result.current.has('graylog_1')).toBe(false);
  });

  it('queries the catalog scoped to exact matches of the given index names', async () => {
    asMock(fetch).mockResolvedValue({ archives: [] });

    renderHook(() => useArchivedIndexNames(['graylog_1', 'graylog_0'], true));

    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(1));

    const [method, url] = asMock(fetch).mock.calls[0];
    // The query is percent-encoded (encodeURIComponent), so decode before asserting on it.
    const decodedQuery = decodeURIComponent(url as string);

    expect(method).toBe('GET');
    expect(url).toContain('/plugins/org.graylog.plugins.archive/cluster/archives/catalog');
    // Names are sorted for a stable cache key and matched with the exact-match operator.
    expect(decodedQuery).toContain('query=index:=graylog_0 index:=graylog_1');
    expect(decodedQuery).toContain('per_page=10000');
  });

  it('does not query when archiving is unavailable', () => {
    renderHook(() => useArchivedIndexNames(['graylog_0'], false));

    expect(fetch).not.toHaveBeenCalled();
  });

  it('does not query when there are no index names to look up', () => {
    renderHook(() => useArchivedIndexNames([], true));

    expect(fetch).not.toHaveBeenCalled();
  });
});
