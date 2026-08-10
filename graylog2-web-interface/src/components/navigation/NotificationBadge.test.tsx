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
import Immutable from 'immutable';
import { render, screen, waitFor } from 'wrappedTestingLibrary';

import { asMock } from 'helpers/mocking';
import { adminUser } from 'fixtures/users';
import useCurrentUser from 'hooks/useCurrentUser';
import useNotificationBadgeCount from 'components/notifications/hooks/useNotificationBadgeCount';

import NotificationBadge from './NotificationBadge';

const BADGE_ID = 'notification-badge';

jest.mock('hooks/useCurrentUser');
jest.mock('components/notifications/hooks/useNotificationBadgeCount');

const setBadgeCount = (count: number) =>
  asMock(useNotificationBadgeCount).mockReturnValue({
    data: count,
    isLoading: false,
  });

describe('NotificationBadge', () => {
  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(adminUser);
    setBadgeCount(0);
  });

  it('renders nothing when user has no notification permissions', () => {
    const userWithoutPermissions = adminUser
      .toBuilder()
      .permissions(Immutable.List(['dashboards:read']))
      .build();
    asMock(useCurrentUser).mockReturnValue(userWithoutPermissions);

    render(<NotificationBadge />);

    expect(useNotificationBadgeCount).toHaveBeenCalledWith({ enabled: false });
    expect(screen.queryByTestId(BADGE_ID)).not.toBeInTheDocument();
  });

  it('links to the system notifications page', async () => {
    render(<NotificationBadge />);

    expect(await screen.findByRole('link')).toHaveAttribute('href', '/system/notifications');
  });

  it('shows the icon without a count when there are no unread notifications', async () => {
    render(<NotificationBadge />);

    const badge = await screen.findByTestId(BADGE_ID);

    expect(badge).toHaveAccessibleName('No unread system notifications');
    expect(badge).not.toHaveTextContent('0');
  });

  it('shows no count while loading', async () => {
    asMock(useNotificationBadgeCount).mockReturnValue({ data: 0, isLoading: true });

    render(<NotificationBadge />);

    expect(await screen.findByTestId(BADGE_ID)).toHaveAccessibleName('No unread system notifications');
  });

  it('renders count when there are unread notifications', async () => {
    setBadgeCount(42);

    render(<NotificationBadge />);

    const badge = await screen.findByTestId(BADGE_ID);

    expect(badge).toHaveTextContent('42');
    expect(badge).toHaveAccessibleName('42 unread system notifications');
  });

  it('uses a singular accessible name for a single notification', async () => {
    setBadgeCount(1);

    render(<NotificationBadge />);

    expect(await screen.findByTestId(BADGE_ID)).toHaveAccessibleName('1 unread system notification');
  });

  it('caps the displayed count', async () => {
    setBadgeCount(120);

    render(<NotificationBadge />);

    const badge = await screen.findByTestId(BADGE_ID);

    expect(badge).toHaveTextContent('99+');
    expect(badge).toHaveAccessibleName('120 unread system notifications');
  });

  it('updates the badge count on subsequent polls', async () => {
    setBadgeCount(42);

    const { rerender } = render(<NotificationBadge />);

    expect(await screen.findByTestId(BADGE_ID)).toHaveTextContent('42');

    setBadgeCount(23);

    rerender(<NotificationBadge />);

    await waitFor(async () => {
      expect(await screen.findByTestId(BADGE_ID)).toHaveTextContent('23');
    });
  });
});
