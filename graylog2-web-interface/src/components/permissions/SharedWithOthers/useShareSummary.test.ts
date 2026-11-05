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

import { AuthorizationShares } from '@graylog/server-api';

import { asMock } from 'helpers/mocking';
import UserNotification from 'util/UserNotification';

import useShareSummary from './useShareSummary';

jest.mock('@graylog/server-api', () => ({
  AuthorizationShares: {
    getSummaries: jest.fn(),
  },
}));

jest.mock('util/UserNotification', () => ({ error: jest.fn() }));

const renderHooks = (grns: Array<string>) => renderHook(() => grns.map((grn) => useShareSummary(grn)));

describe('useShareSummary', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('sends one request for the entities rendered together', async () => {
    asMock(AuthorizationShares.getSummaries).mockResolvedValue({
      summaries: { 'grn::::stream:batch-1': { own: 1, manage: 0, view: 3 } },
    });

    const { result } = renderHooks(['grn::::stream:batch-1', 'grn::::dashboard:batch-2']);

    await waitFor(() => expect(result.current.every(({ isInitialLoading }) => !isInitialLoading)).toBe(true));

    expect(AuthorizationShares.getSummaries).toHaveBeenCalledTimes(1);
    expect(AuthorizationShares.getSummaries).toHaveBeenCalledWith({
      grns: expect.arrayContaining(['grn::::stream:batch-1', 'grn::::dashboard:batch-2']),
    });
    expect(result.current[0].data).toEqual({ own: 1, manage: 0, view: 3 });
  });

  it('returns null for an entity left out of the response', async () => {
    asMock(AuthorizationShares.getSummaries).mockResolvedValue({ summaries: {} });

    const { result } = renderHooks(['grn::::stream:missing-1']);

    await waitFor(() => expect(result.current[0].isInitialLoading).toBe(false));

    expect(result.current[0].data).toBeNull();
    expect(result.current[0].isError).toBe(false);
  });

  it('shows one notification and does not retry when the request fails', async () => {
    asMock(AuthorizationShares.getSummaries).mockRejectedValue(new Error('Internal Server Error'));

    const { result } = renderHooks(['grn::::stream:failing-1', 'grn::::stream:failing-2']);

    await waitFor(() => expect(result.current.every(({ isError }) => isError)).toBe(true));

    expect(UserNotification.error).toHaveBeenCalledTimes(1);
    expect(AuthorizationShares.getSummaries).toHaveBeenCalledTimes(1);
  });
});
