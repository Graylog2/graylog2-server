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
import { render, screen, waitFor, within } from 'wrappedTestingLibrary';

import { asMock } from 'helpers/mocking';
import useNotifications from 'components/notifications/useNotifications';

import NotificationBadge from './NotificationBadge';

const BADGE_ID = 'notification-badge';

jest.mock('components/notifications/useNotifications');

const notificationFixture = {
  id: 'deadbeef',
  details: {},
  validations: {},
  fields: {},
  severity: 'urgent',
  type: 'no_input_running',
  key: 'test',
  timestamp: '2022-12-12T10:55:55.014Z',
  node_id: '3fcc3889-18a3-4a0d-821c-0fd560d152e7',
} as const;

const createNotifications = (count: number) => new Array(count).fill(notificationFixture);

const setNotificationCount = (count: number) =>
  asMock(useNotifications).mockReturnValue({
    data: { total: count, notifications: createNotifications(count) },
    isLoading: false,
  });

describe('NotificationBadge', () => {
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

    const { rerender } = render(<NotificationBadge />);

    const badgeBefore = await screen.findByTestId(BADGE_ID);

    expect(within(badgeBefore).getByText(42)).toBeInTheDocument();

    setNotificationCount(23);

    rerender(<NotificationBadge />);

    const badgeAfter = await screen.findByTestId(BADGE_ID);

    await waitFor(() => {
      expect(within(badgeAfter).getByText(23)).toBeInTheDocument();
    });
  });
});
