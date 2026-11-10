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
import * as Immutable from 'immutable';
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import type { Permission } from 'graylog-web-plugin/plugin';

import { Sidecar } from '@graylog/server-api';

import { asMock } from 'helpers/mocking';
import mockComponent from 'helpers/mocking/MockComponent';
import { adminUser } from 'fixtures/users';
import useCurrentUser from 'hooks/useCurrentUser';
import UserNotification from 'util/UserNotification';
import FetchError from 'logic/errors/FetchError';
import SidecarsPage from 'pages/SidecarsPage';

jest.mock('hooks/useCurrentUser');
jest.mock('components/sidecars/sidecars/SidecarListContainer', () => mockComponent('SidecarListContainer'));
jest.mock('components/sidecars/common/SidecarsPageNavigation', () => mockComponent('SidecarsPageNavigation'));

jest.mock('@graylog/server-api', () => ({
  Sidecar: {
    getBasicSidecarUser: jest.fn(),
  },
}));

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

const sidecarUserResponse = {
  id: 'sidecar-user-id',
  username: 'my-custom-sidecar',
  full_name: 'Sidecar System User',
  read_only: false,
  service_account: true,
};

const userWithPermissions = (permissions: Array<Permission>) =>
  adminUser.toBuilder().permissions(Immutable.List(permissions)).build();

describe('SidecarsPage', () => {
  it('shows the API token hint for the configured sidecar user', async () => {
    asMock(useCurrentUser).mockReturnValue(
      userWithPermissions(['sidecars:read', 'users:read:my-custom-sidecar', 'users:tokencreate:my-custom-sidecar']),
    );

    asMock(Sidecar.getBasicSidecarUser).mockResolvedValue(sidecarUserResponse);

    render(<SidecarsPage />);

    await screen.findByRole('link', { name: /create or reuse a token for the my-custom-sidecar user/i });
  });

  it('does not show the API token hint and stays silent when reading the sidecar user is forbidden', async () => {
    asMock(useCurrentUser).mockReturnValue(userWithPermissions(['sidecars:read']));

    asMock(Sidecar.getBasicSidecarUser).mockRejectedValue(
      new FetchError('Forbidden', 403, 'Not allowed to view user my-custom-sidecar'),
    );

    render(<SidecarsPage />);

    await waitFor(() => expect(Sidecar.getBasicSidecarUser).toHaveBeenCalled());

    expect(screen.queryByText(/do you need an api token for a sidecar\?/i)).not.toBeInTheDocument();
    expect(UserNotification.error).not.toHaveBeenCalled();
  });

  it('does not show the API token hint without permission to create tokens for the sidecar user', async () => {
    asMock(useCurrentUser).mockReturnValue(userWithPermissions(['sidecars:read', 'users:read:my-custom-sidecar']));

    asMock(Sidecar.getBasicSidecarUser).mockResolvedValue(sidecarUserResponse);

    render(<SidecarsPage />);

    await waitFor(() => expect(Sidecar.getBasicSidecarUser).toHaveBeenCalled());

    expect(screen.queryByText(/do you need an api token for a sidecar\?/i)).not.toBeInTheDocument();
  });
});
