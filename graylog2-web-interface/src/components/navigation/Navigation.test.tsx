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
import { render, screen } from 'wrappedTestingLibrary';
import type { Location } from 'history';
import { defaultUser } from 'defaultMockValues';

import mockComponent from 'helpers/mocking/MockComponent';
import { asMock } from 'helpers/mocking';
import Navigation from 'components/navigation/Navigation';
import useCurrentUser from 'hooks/useCurrentUser';
import useLocation from 'routing/useLocation';
import HotkeysProvider from 'contexts/HotkeysProvider';
import useNotifications from 'components/notifications/useNotifications';
import { alice } from 'fixtures/users';

jest.mock('./ScratchpadToggle', () => mockComponent('ScratchpadToggle'));
jest.mock('hooks/useCurrentUser');
jest.mock('routing/useLocation', () => jest.fn(() => ({ pathname: '' })));
jest.mock('@graylog/server-api', () => ({
  SystemNotifications: {
    listNotifications: jest.fn(async () => ({ total: 0 })),
  },
}));
jest.mock('components/notifications/useNotifications');

describe('Navigation', () => {
  const SUT = () => (
    <HotkeysProvider>
      <Navigation />
    </HotkeysProvider>
  );

  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(defaultUser);
    asMock(useLocation).mockReturnValue({ pathname: '/' } as Location);

    asMock(useNotifications).mockReturnValue({
      data: { total: 1, notifications: [{ type: 'no_input_running', key: 'test', timestamp: '2022-12-12T10:55:55.014Z' }] },
      isLoading: false,
    });
  });

  it('has common elements', async () => {
    render(<SUT />);

    await screen.findByRole('link', { name: /throughput/i });
    await screen.findByRole('button', { name: /help/i });
    await screen.findByRole('button', { name: /user menu for administrator/i });
  });

  it('shows notification badge for users with notifications:read permission', async () => {
    render(<SUT />);

    await screen.findByTestId('notification-badge');
  });

  it('does not show notification badge for users without notifications:read permission', async () => {
    asMock(useCurrentUser).mockReturnValue(alice);

    render(<SUT />);

    await screen.findByRole('button', { name: /help/i });

    expect(screen.queryByTestId('notification-badge')).not.toBeInTheDocument();
  });
});
