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

import { SystemCatalog } from '@graylog/server-api';

import asMock from 'helpers/mocking/AsMock';

import useNotificationsByIds from './useNotificationsByIds';

jest.mock('@graylog/server-api', () => ({
  SystemCatalog: {
    getTitles: jest.fn(),
  },
}));

const notification = (id: string, title: string) => ({ id, type: 'event_notifications', title });

describe('useNotificationsByIds', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('resolves the notifications of hooks rendered together with one request', async () => {
    asMock(SystemCatalog.getTitles).mockResolvedValue({
      entities: [notification('n1', 'Slack'), notification('n2', 'Email'), notification('n3', 'PagerDuty')],
      not_permitted_to_view: [],
    });

    const { result } = renderHook(() => [useNotificationsByIds(['n1', 'n2']), useNotificationsByIds(['n2', 'n3'])]);

    await waitFor(() => expect(result.current.every(({ isLoading }) => !isLoading)).toBe(true));

    expect(SystemCatalog.getTitles).toHaveBeenCalledTimes(1);
    expect(
      asMock(SystemCatalog.getTitles)
        .mock.calls[0][0].entities.map(({ id }) => id)
        .sort(),
    ).toEqual(['n1', 'n2', 'n3']);
    expect(result.current[0].data).toEqual([
      { id: 'n1', title: 'Slack' },
      { id: 'n2', title: 'Email' },
    ]);
    expect(result.current[1].data).toEqual([
      { id: 'n2', title: 'Email' },
      { id: 'n3', title: 'PagerDuty' },
    ]);
  });

  it('returns notifications the user is not permitted to view separately and skips missing ones', async () => {
    asMock(SystemCatalog.getTitles).mockResolvedValue({
      entities: [notification('n4', 'Slack')],
      not_permitted_to_view: ['secret'],
    });

    const { result } = renderHook(() => useNotificationsByIds(['n4', 'secret', 'missing']));

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.data).toEqual([{ id: 'n4', title: 'Slack' }]);
    expect(result.current.notPermittedIds).toEqual(['secret']);
  });
});
