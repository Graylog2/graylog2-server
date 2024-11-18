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
import * as React from 'react';
import { render, screen, waitFor, act, within } from 'wrappedTestingLibrary';

import { MockStore, asMock } from 'helpers/mocking';
import { NotificationsActions, NotificationsStore } from 'stores/notifications/NotificationsStore';

import NotificationBadge from './NotificationBadge';

jest.mock('stores/notifications/NotificationsStore', () => ({
  NotificationsActions: { list: jest.fn() },
  NotificationsStore: MockStore(),
}));

const BADGE_ID = 'notification-badge';

type NotificationsStoreType = ReturnType<typeof NotificationsStore.getInitialState>;

const setNotificationCount = (count: number) => {
  asMock(NotificationsStore.getInitialState).mockReturnValue({ total: count } as NotificationsStoreType);
};

describe('NotificationBadge', () => {
  beforeAll(() => {
    jest.useFakeTimers();
  });

  afterAll(() => {
    jest.useRealTimers();
  });

  beforeEach(() => {
    jest.restoreAllMocks();
    jest.clearAllMocks();
  });

  it('triggers update of notifications', async () => {
    expect(NotificationsActions.list).not.toHaveBeenCalled();

    render(<NotificationBadge />);

    jest.advanceTimersByTime(3000);
    await waitFor(() => { expect(NotificationsActions.list).toHaveBeenCalled(); });
  });

  it('renders nothing when there are no notifications', () => {
    setNotificationCount(0);
    render(<NotificationBadge />);

    expect(screen.queryByTestId(BADGE_ID)).not.toBeInTheDocument();
  });

  it('renders count when there are notifications', async () => {
    setNotificationCount(42);

    render(<NotificationBadge />);

    await screen.findByTestId(BADGE_ID);
    const badge = await screen.findByTestId(BADGE_ID);

    expect(within(badge).getByText(42)).toBeInTheDocument();
  });

  it('updates notification count when triggered by store', async () => {
    setNotificationCount(42);

    render(<NotificationBadge />);

    const badgeBefore = await screen.findByTestId(BADGE_ID);

    expect(within(badgeBefore).getByText(42)).toBeInTheDocument();

    const cb = asMock(NotificationsStore.listen).mock.calls[0][0];

    act(() => {
      cb({ total: 23 } as NotificationsStoreType);
    });

    const badgeAfter = await screen.findByTestId(BADGE_ID);

    await waitFor(() => {
      expect(within(badgeAfter).getByText(23)).toBeInTheDocument();
    });
  });
});
