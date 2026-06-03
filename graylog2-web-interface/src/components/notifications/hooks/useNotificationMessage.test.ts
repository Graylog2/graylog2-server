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
import { useQuery } from '@tanstack/react-query';

import asMock from 'helpers/mocking/AsMock';
import type { NotificationType } from 'components/notifications/types';

import useNotificationMessage from './useNotificationMessage';

jest.mock('@tanstack/react-query', () => ({
  ...jest.requireActual('@tanstack/react-query'),
  useQuery: jest.fn(() => ({ data: undefined })),
}));

jest.mock('@graylog/server-api', () => ({
  SystemNotificationMessage: {
    renderHtml: jest.fn(),
    renderHtmlWithKey: jest.fn(),
  },
}));

jest.mock('components/notifications/NotificationsFactory', () => ({
  __esModule: true,
  default: { getValuesForNotification: jest.fn(() => ({})) },
}));

const baseNotification: NotificationType = {
  id: 'first-id',
  type: 'no_input_running',
  key: '',
  details: {},
  severity: 'urgent',
  timestamp: '2026-01-01T00:00:00.000Z',
  node_id: 'node-1',
  title: '',
  description: '',
};

describe('useNotificationMessage', () => {
  beforeEach(() => {
    asMock(useQuery).mockClear();
  });

  it('includes notification id, key, and type in the React Query entry key', () => {
    renderHook(() => useNotificationMessage(baseNotification));

    expect(useQuery).toHaveBeenCalledTimes(1);
    const queryKey = asMock(useQuery).mock.calls[0][0].queryKey;

    expect(queryKey.slice(0, 4)).toEqual(['system', 'notifications', 'message', 'first-id']);
    expect(queryKey).toContain('no_input_running');
  });

  it('produces a different cache key for two notifications with the same type but different ids', () => {
    const second = { ...baseNotification, id: 'second-id' } as unknown as NotificationType;

    renderHook(() => useNotificationMessage(baseNotification));
    renderHook(() => useNotificationMessage(second));

    const firstKey = asMock(useQuery).mock.calls[0][0].queryKey;
    const secondKey = asMock(useQuery).mock.calls[1][0].queryKey;

    expect(firstKey).not.toEqual(secondKey);
    expect(firstKey).toContain('first-id');
    expect(secondKey).toContain('second-id');
  });
});
