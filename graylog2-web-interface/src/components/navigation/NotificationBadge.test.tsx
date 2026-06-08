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
import { render, screen, waitFor, within } from 'wrappedTestingLibrary';

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
  });

  it('renders nothing when user has no notification permissions', () => {
    const userWithoutPermissions = adminUser
      .toBuilder()
      .permissions(Immutable.List(['dashboards:read']))
      .build();
    asMock(useCurrentUser).mockReturnValue(userWithoutPermissions);
    asMock(useNotificationBadgeCount).mockReturnValue({ data: undefined, isLoading: false } as never);

    render(<NotificationBadge />);

    expect(useNotificationBadgeCount).toHaveBeenCalledWith({ enabled: false });
    expect(screen.queryByTestId(BADGE_ID)).not.toBeInTheDocument();
  });

  it('renders nothing while loading', () => {
    asMock(useNotificationBadgeCount).mockReturnValue({ data: undefined, isLoading: true } as never);

    render(<NotificationBadge />);

    expect(screen.queryByTestId(BADGE_ID)).not.toBeInTheDocument();
  });

  it('renders count when there are unread notifications', async () => {
    setBadgeCount(42);

    render(<NotificationBadge />);

    const badge = await screen.findByTestId(BADGE_ID);

    expect(within(badge).getByText(42)).toBeInTheDocument();
  });

  it('updates the badge count on subsequent polls', async () => {
    setBadgeCount(42);

    const { rerender } = render(<NotificationBadge />);

    const badgeBefore = await screen.findByTestId(BADGE_ID);

    expect(within(badgeBefore).getByText(42)).toBeInTheDocument();

    setBadgeCount(23);

    rerender(<NotificationBadge />);

    const badgeAfter = await screen.findByTestId(BADGE_ID);

    await waitFor(() => {
      expect(within(badgeAfter).getByText(23)).toBeInTheDocument();
    });
  });
});
