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
import { OrderedMap } from 'immutable';

import { Collectors } from '@graylog/server-api';

import asMock from 'helpers/mocking/AsMock';
import type { SearchParams } from 'stores/PaginationTypes';

import { fetchPaginatedInstances, useInstance } from './useInstanceQueries';

jest.mock('@graylog/server-api', () => ({
  Collectors: {
    getInstance: jest.fn(),
    findInstances: jest.fn(),
  },
}));

const dto = (instanceUid: string) => ({
  instance_uid: instanceUid,
  fleet_id: 'fleet-1',
  capabilities: 0,
  enrolled_at: '2026-06-10T12:00:00Z',
  last_seen: '2026-06-10T12:01:00Z',
  status: 'online',
  active_certificate_fingerprint: null,
  active_certificate_expires_at: null,
  next_certificate_fingerprint: null,
  next_certificate_expires_at: null,
  identifying_attributes: { 'host.name': `host-${instanceUid}` },
  non_identifying_attributes: { 'service.version': '1.2.3', 'os.type': 'linux' },
});

const asInstanceResponse = (instanceUid: string) =>
  dto(instanceUid) as unknown as Awaited<ReturnType<typeof Collectors.getInstance>>;

describe('useInstance', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('returns the matching instance mapped to a view', async () => {
    asMock(Collectors.getInstance).mockResolvedValue(asInstanceResponse('uid-42'));

    const { result } = renderHook(() => useInstance('uid-42'));

    await waitFor(() => expect(result.current.data).toBeTruthy());

    // The query polls, so it must not keep an idle user's session alive.
    expect(Collectors.getInstance).toHaveBeenCalledWith('uid-42', { requestShouldExtendSession: false });

    expect(result.current.data).toEqual(
      expect.objectContaining({
        id: 'uid-42',
        instance_uid: 'uid-42',
        fleet_id: 'fleet-1',
        hostname: 'host-uid-42',
        version: '1.2.3',
      }),
    );
  });

  // The GET endpoint throws NotFoundException (HTTP 404) for a missing instance, so a
  // failed lookup now travels the react-query error path instead of resolving to null.
  it('surfaces an error when the instance cannot be loaded', async () => {
    asMock(Collectors.getInstance).mockRejectedValue(new Error('Collector instance <uid-unknown> not found'));

    const { result } = renderHook(() => useInstance('uid-unknown'));

    await waitFor(() => expect(result.current.isError).toBe(true));

    expect(result.current.data).toBeUndefined();
  });

  it('does not fetch without an instance uid', () => {
    renderHook(() => useInstance(undefined));

    expect(Collectors.getInstance).not.toHaveBeenCalled();
  });

  it('maps health to the view', async () => {
    const health = {
      healthy_changed_at: '2026-07-31T10:00:00.000+0000',
      component_health: { healthy: false, last_error: 'connection refused' },
    };
    asMock(Collectors.getInstance).mockResolvedValue({
      ...dto('uid-42'),
      health,
    } as unknown as Awaited<ReturnType<typeof Collectors.getInstance>>);

    const { result } = renderHook(() => useInstance('uid-42'));

    await waitFor(() => expect(result.current.data).toBeTruthy());

    expect(result.current.data.health).toEqual(health);
  });

  it('normalizes absent health to null', async () => {
    asMock(Collectors.getInstance).mockResolvedValue(asInstanceResponse('uid-42'));

    const { result } = renderHook(() => useInstance('uid-42'));

    await waitFor(() => expect(result.current.data).toBeTruthy());

    expect(result.current.data.health).toBeNull();
  });

  // A collector that stops reporting in is marked offline server-side, with no user interaction to
  // trigger a refetch. Without polling, a page rendered while the collector was healthy keeps
  // asserting "online" forever.
  it('picks up a status change without any user interaction', async () => {
    jest.useFakeTimers();

    asMock(Collectors.getInstance).mockResolvedValue(asInstanceResponse('uid-42'));

    const { result } = renderHook(() => useInstance('uid-42'));

    await waitFor(() => expect(result.current.data?.status).toBe('online'));

    asMock(Collectors.getInstance).mockResolvedValue({
      ...dto('uid-42'),
      status: 'offline',
    } as unknown as Awaited<ReturnType<typeof Collectors.getInstance>>);

    // Past the fallback heartbeat cadence used until the collectors config has loaded. Wrapped in
    // `act` because the refetch it triggers re-renders the hook outside React's own scheduling.
    await act(async () => {
      await jest.advanceTimersByTimeAsync(31_000);
    });

    await waitFor(() => expect(result.current.data?.status).toBe('offline'));

    jest.useRealTimers();
  });
});

describe('fetchPaginatedInstances session extension', () => {
  const searchParams = (page: number): SearchParams => ({
    page,
    pageSize: 50,
    query: '',
    sort: { attributeId: 'last_seen', direction: 'desc' },
    filters: OrderedMap(),
  });

  const lastRequestOptions = () => asMock(Collectors.findInstances).mock.calls.at(-1)?.at(-1);

  beforeEach(() => {
    jest.clearAllMocks();
    asMock(Collectors.findInstances).mockResolvedValue({
      elements: [],
      pagination: { total: 0, page: 1, per_page: 50, count: 0 },
      attributes: [],
    } as unknown as Awaited<ReturnType<typeof Collectors.findInstances>>);
  });

  it('extends the session for user interactions but not for background refreshes', async () => {
    // Initial load: user navigated to the table.
    await fetchPaginatedInstances(searchParams(1));
    expect(lastRequestOptions()).toEqual({ requestShouldExtendSession: true });

    // Interval refresh: same params as before.
    await fetchPaginatedInstances(searchParams(1));
    expect(lastRequestOptions()).toEqual({ requestShouldExtendSession: false });

    // User pages to page 2.
    await fetchPaginatedInstances(searchParams(2));
    expect(lastRequestOptions()).toEqual({ requestShouldExtendSession: true });

    // Interval refresh of page 2.
    await fetchPaginatedInstances(searchParams(2));
    expect(lastRequestOptions()).toEqual({ requestShouldExtendSession: false });

    // User pages back to page 1 — a repeat of earlier params still counts as an interaction,
    // only consecutive repeats are background refreshes.
    await fetchPaginatedInstances(searchParams(1));
    expect(lastRequestOptions()).toEqual({ requestShouldExtendSession: true });
  });
});
