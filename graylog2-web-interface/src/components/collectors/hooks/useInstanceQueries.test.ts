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

import { Collectors } from '@graylog/server-api';

import asMock from 'helpers/mocking/AsMock';

import { useInstance } from './useInstanceQueries';

jest.mock('@graylog/server-api', () => ({
  Collectors: {
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

describe('useInstance', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    asMock(Collectors.findInstances).mockResolvedValue({
      elements: [dto('uid-1'), dto('uid-42')],
      pagination: { total: 2 },
      attributes: [],
    } as unknown as Awaited<ReturnType<typeof Collectors.findInstances>>);
  });

  it('returns the matching instance mapped to a view', async () => {
    const { result } = renderHook(() => useInstance('uid-42'));

    await waitFor(() => expect(result.current.data).toBeTruthy());

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

  it('returns null when no instance matches', async () => {
    const { result } = renderHook(() => useInstance('uid-unknown'));

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.data).toBeNull();
  });

  it('does not fetch without an instance uid', () => {
    renderHook(() => useInstance(undefined));

    expect(Collectors.findInstances).not.toHaveBeenCalled();
  });
});
