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
import { render, screen, waitFor } from 'wrappedTestingLibrary';

import { asMock } from 'helpers/mocking';
import { NotificationsActions } from 'stores/notifications/NotificationsStore';

import NotificationBadge from './NotificationBadge';

jest.mock('stores/notifications/NotificationsStore', () => ({
  NotificationsActions: { list: jest.fn(async () => ({ total: 0 })) },
}));

const BADGE_ID = 'notification-badge';

describe('NotificationBadge', () => {
  beforeAll(() => {
    jest.useFakeTimers();
  });

  afterAll(() => {
    jest.useRealTimers();
  });

  it('triggers update of notifications', async () => {
    expect(NotificationsActions.list).not.toHaveBeenCalled();

    render(<NotificationBadge />);

    await waitFor(() => { expect(NotificationsActions.list).toHaveBeenCalled(); });

    asMock(NotificationsActions.list).mockClear();

    jest.advanceTimersByTime(3000);
    await waitFor(() => { expect(NotificationsActions.list).toHaveBeenCalled(); });
  });

  it('renders count when there are notifications', async () => {
    asMock(NotificationsActions.list).mockResolvedValue({ total: 42 });

    render(<NotificationBadge />);

    await screen.findByTestId(BADGE_ID);
    const badge = await screen.findByTestId(BADGE_ID);

    expect(badge.innerHTML).toEqual('42');
  });

  it('updates notification count when triggered by store', async () => {
    asMock(NotificationsActions.list).mockResolvedValue({ total: 42 });

    render(<NotificationBadge />);

    const badgeBefore = await screen.findByTestId(BADGE_ID);

    expect(badgeBefore.innerHTML).toEqual('42');

    asMock(NotificationsActions.list).mockResolvedValue({ total: 23 });

    jest.advanceTimersByTime(3000);

    const badgeAfter = await screen.findByTestId(BADGE_ID);

    await waitFor(() => { expect(badgeAfter.innerHTML).toEqual('23'); });
  });

  it('renders nothing when there are no notifications', () => {
    asMock(NotificationsActions.list).mockResolvedValue({ total: 0 });

    render(<NotificationBadge />);

    expect(screen.queryByTestId(BADGE_ID)).not.toBeInTheDocument();
  });
});
