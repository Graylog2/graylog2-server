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
import suppressConsole from 'helpers/suppressConsole';

import useDataNodes from './useDataNodes';

jest.mock('logic/rest/FetchProvider', () => jest.fn());

jest.mock('preflight/util/UserNotification', () => ({
  error: jest.fn(),
}));

describe('useDataNodes', () => {
  const availableDataNodes = [
    {
      id: 'data-node-id-1',
      name: 'data-node-name',
      transportAddress: 'transport.address1',
      altNames: [],
      status: 'UNCONFIGURED',
    },
    {
      id: 'data-node-id-2',
      name: 'data-node-name',
      altNames: [],
      transportAddress: 'transport.address2',
      status: 'UNCONFIGURED',
    },
    {
      id: 'data-node-id-3',
      name: 'data-node-name',
      altNames: [],
      transportAddress: 'transport.address3',
      status: 'UNCONFIGURED',
    },
  ];

  beforeEach(() => {
    asMock(fetch).mockReturnValue(Promise.resolve(availableDataNodes));
  });

  it('should return data nodes CA status', async () => {
    const { result, waitFor } = renderHook(() => useDataNodes());

    expect(result.current.data).toEqual([]);

    await waitFor(() => result.current.isFetching);
    await waitFor(() => !result.current.isFetching);

    expect(fetch).toHaveBeenCalledWith('GET', expect.stringContaining('/api/data_nodes'), undefined, false);

    await waitFor(() => expect(result.current.data).toEqual(availableDataNodes));
  });

  it('should return fetch error', async () => {
    asMock(fetch).mockReturnValue(Promise.reject(new Error('Error')));

    const { result, waitFor } = renderHook(() => useDataNodes());

    expect(result.current.data).toEqual([]);

    suppressConsole(async () => {
      await waitFor(() => result.current.isFetching);
      await waitFor(() => !result.current.isFetching);
    });

    await waitFor(() => expect(result.current.error).toEqual(new Error('Error')));

    expect(result.current.data).toEqual([]);
  });
});
