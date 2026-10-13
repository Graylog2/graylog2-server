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
import { act, renderHook, waitFor } from 'wrappedTestingLibrary/hooks';

import { dataNodes } from 'fixtures/dataNodes';
import asMock from 'helpers/mocking/AsMock';
import { fetchPeriodically } from 'logic/rest/FetchProvider';

import useDataNode from './useDataNode';

jest.mock('logic/rest/FetchProvider');

describe('useDataNode', () => {
  afterEach(() => {
    jest.clearAllMocks();
    jest.useRealTimers();
  });

  it('keeps cached details separate for each node', async () => {
    asMock(fetchPeriodically)
      .mockResolvedValueOnce(dataNodes[0])
      .mockResolvedValueOnce(dataNodes[1])
      .mockResolvedValue(dataNodes[0]);
    const { result, rerender } = renderHook(({ nodeId }) => useDataNode(nodeId), {
      initialProps: { nodeId: dataNodes[0].node_id },
    });
    await waitFor(() => expect(result.current.data?.node_id).toBe(dataNodes[0].node_id));
    rerender({ nodeId: dataNodes[1].node_id });
    await waitFor(() => expect(result.current.data?.node_id).toBe(dataNodes[1].node_id));
    rerender({ nodeId: dataNodes[0].node_id });
    expect(result.current.data?.node_id).toBe(dataNodes[0].node_id);
    expect(fetchPeriodically).toHaveBeenCalledWith('GET', expect.stringContaining(`/datanode/${dataNodes[1].node_id}`));
  });

  it('uses polling requests for periodic refreshes', async () => {
    jest.useFakeTimers();
    asMock(fetchPeriodically).mockResolvedValue(dataNodes[0]);
    const { result, unmount } = renderHook(() => useDataNode(dataNodes[0].node_id));
    await waitFor(() => expect(result.current.data?.node_id).toBe(dataNodes[0].node_id));
    asMock(fetchPeriodically).mockClear();
    await act(async () => {
      await jest.advanceTimersByTimeAsync(5000);
    });
    expect(fetchPeriodically).toHaveBeenCalledWith('GET', expect.stringContaining(`/datanode/${dataNodes[0].node_id}`));
    unmount();
  });
});
