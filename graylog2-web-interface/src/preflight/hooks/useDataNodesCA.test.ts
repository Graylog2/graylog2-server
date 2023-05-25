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

import asMock from 'helpers/mocking/AsMock';
import fetch from 'logic/rest/FetchProvider';

import useDataNodesCA from './useDataNodesCA';

jest.mock('logic/rest/FetchProvider', () => jest.fn());

describe('useDataNodesCA', () => {
  beforeEach(() => {
    asMock(fetch).mockReturnValue(Promise.resolve({ id: 'ca-id', type: 'ca-type' }));
  });

  it('should return fetched data nodes', async () => {
    const { result, waitFor } = renderHook(() => useDataNodesCA());

    expect(result.current.data).toEqual(undefined);

    await waitFor(() => result.current.isFetching);
    await waitFor(() => !result.current.isFetching);

    expect(fetch).toHaveBeenCalledWith('GET', expect.stringContaining('/api/ca'), undefined, false);

    await waitFor(() => expect(result.current.data).toEqual({ id: 'ca-id', type: 'ca-type' }));
  });
});
