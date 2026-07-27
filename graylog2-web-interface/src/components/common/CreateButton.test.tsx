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
import { render, screen } from 'wrappedTestingLibrary';

import { adminUser } from 'fixtures/users';
import { asMock } from 'helpers/mocking';
import useCurrentUser from 'hooks/useCurrentUser';
import usePluginEntities from 'hooks/usePluginEntities';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import type { QualifiedUrl } from 'routing/Routes';

import CreateButton from './CreateButton';

jest.mock('hooks/useCurrentUser');
jest.mock('hooks/usePluginEntities');
jest.mock('logic/telemetry/useSendTelemetry');
jest.mock('routing/useLocation', () => jest.fn(() => ({ pathname: '/test' })));

const entityCreatorWithPermissions = {
  id: 'Stream',
  title: 'Create stream',
  path: '/streams/new' as QualifiedUrl<string>,
  permissions: 'streams:create',
};

const entityCreatorWithoutPermissions = {
  id: 'Content Pack',
  title: 'Create content pack',
  path: '/system/contentpacks/create' as QualifiedUrl<string>,
};

describe('CreateButton', () => {
  beforeEach(() => {
    asMock(usePluginEntities).mockReturnValue([entityCreatorWithPermissions, entityCreatorWithoutPermissions]);
    asMock(useCurrentUser).mockReturnValue(adminUser);
    asMock(useSendTelemetry).mockReturnValue(jest.fn());
  });

  it('renders button for user with the required permission', () => {
    const user = adminUser.toBuilder().permissions(Immutable.List(['streams:create'])).build();
    asMock(useCurrentUser).mockReturnValue(user);

    render(<CreateButton entityKey="Stream" />);

    expect(screen.getByText(/Create stream/)).toBeInTheDocument();
  });

  it('does not render button for user without the required permission', () => {
    const user = adminUser.toBuilder().permissions(Immutable.List([])).build();
    asMock(useCurrentUser).mockReturnValue(user);

    render(<CreateButton entityKey="Stream" />);

    expect(screen.queryByText(/Create stream/)).not.toBeInTheDocument();
  });

  it('renders button when no permissions are required on the entity creator', () => {
    const user = adminUser.toBuilder().permissions(Immutable.List([])).build();
    asMock(useCurrentUser).mockReturnValue(user);

    render(<CreateButton entityKey="Content Pack" />);

    expect(screen.getByText(/Create content pack/)).toBeInTheDocument();
  });
});
