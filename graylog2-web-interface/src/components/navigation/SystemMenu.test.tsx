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
import type { Location } from 'history';

import useLocation from 'routing/useLocation';
import { asMock } from 'helpers/mocking';
import useCurrentUser from 'hooks/useCurrentUser';
import { adminUser } from 'fixtures/users';
import usePluginEntities from 'hooks/usePluginEntities';
import SystemMenu from 'components/navigation/SystemMenu';

import AppConfig from '../../util/AppConfig';

jest.mock('util/AppConfig', () => ({
  gl2AppPathPrefix: jest.fn(() => ''),
  gl2ServerUrl: jest.fn(() => undefined),
  gl2DevMode: jest.fn(() => false),
  isCloud: jest.fn(() => false),
}));

jest.mock('hooks/useCurrentUser');
jest.mock('routing/useLocation', () => jest.fn());
jest.mock('hooks/usePluginEntities');

const openSystemMenu = async () => {
  const systemMenu = await screen.findByRole('button', { name: /system/i });
  systemMenu.click();
};

const findSystemMenuItem = (name: string) => screen.findByRole('menuitem', { name });
const querySystemMenuItem = (name: string) => screen.queryByRole('menuitem', { name });

describe('SystemMenu', () => {
  beforeEach(() => {
    asMock(usePluginEntities).mockReturnValue([]);
    asMock(AppConfig.gl2AppPathPrefix).mockReturnValue('');
    asMock(useLocation).mockReturnValue({ pathname: '/' } as Location);
    asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder().permissions(Immutable.List([])).build());
  });

  describe('uses correct permissions:', () => {
    it.each`
    permissions                    | name
    ${[]}                          | ${'Overview'}
    ${[]}                          | ${'Nodes'}
    ${[]}                          | ${'Data Nodes'}
    ${['clusterconfigentry:read']} | ${'Configurations'}
    ${['inputs:read']}             | ${'Inputs'}
    ${['grok_pattern:read']}       | ${'Grok Patterns'}
    ${['outputs:read']}            | ${'Outputs'}
    ${['indices:read']}            | ${'Indices'}
    ${['loggers:read']}            | ${'Logging'}
    ${['authentication:edit']}     | ${'Authentication'}
    ${['users:list']}              | ${'Users and Teams'}
    ${['roles:read']}              | ${'Roles'}
    ${['contentpack:read']}        | ${'Content Packs'}
    ${['lookuptables:read']}       | ${'Lookup Tables'}
    ${['sidecars:read']}           | ${'Sidecars'}
    ${['pipeline:read', 'pipeline_connection:read']} | ${'Pipelines'}
  `('shows $name for user with $permissions permissions', async ({ permissions, name }) => {
      asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder()
        .permissions(Immutable.List(permissions))
        .build());

      render(<SystemMenu />);
      await openSystemMenu();

      await screen.findByRole('menuitem', { name });
    });
  });

  describe('uses items from plugins:', () => {
    const pluginExports = {
      systemnavigation: [
        { path: '/system/licenses', description: 'Licenses', permissions: 'inputs:create' },
        { path: '/system/auditlog', description: 'Audit Log' },
      ],
    };

    beforeEach(() => {
      asMock(usePluginEntities).mockImplementation((key: string) => pluginExports[key]);
    });

    it('includes plugin item in system navigation', async () => {
      render(<SystemMenu />);

      await openSystemMenu();

      const auditLog = await findSystemMenuItem('Audit Log');

      expect(querySystemMenuItem('Licenses')).not.toBeInTheDocument();

      expect(auditLog).toHaveAttribute('href', '/system/auditlog');
    });

    // eslint-disable-next-line jest/expect-expect
    it('includes plugin item in system navigation if required permissions are present', async () => {
      asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder()
        .permissions(Immutable.List(['inputs:create']))
        .build());

      render(<SystemMenu />);
      await openSystemMenu();

      await findSystemMenuItem('Audit Log');
      await findSystemMenuItem('Licenses');
    });

    it('does not include plugin item in system navigation if required permissions are not present', async () => {
      asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder()
        .permissions(Immutable.List([]))
        .build());

      render(<SystemMenu />);
      await openSystemMenu();

      await findSystemMenuItem('Overview');

      expect(querySystemMenuItem('Licenses')).not.toBeInTheDocument();
    });

    it('prefixes plugin path with current application path prefix', async () => {
      asMock(AppConfig.gl2AppPathPrefix).mockReturnValue('/my/fancy/prefix');

      render(<SystemMenu />);
      await openSystemMenu();

      const auditLogLink = await findSystemMenuItem('Audit Log');

      expect(auditLogLink).toHaveAttribute('href', '/my/fancy/prefix/system/auditlog');
    });
  });

  describe('sets a location-specific title for the dropdown', () => {
    const pluginExports = {
      systemnavigation: [
        { path: '/system/licenses', description: 'Licenses', permissions: 'inputs:create' },
        { path: '/system/auditlog', description: 'Audit Log' },
      ],
    };

    beforeEach(() => {
      asMock(usePluginEntities).mockImplementation((key: string) => pluginExports[key]);
    });

    it('uses a default title if location is not matched', async () => {
      render(<SystemMenu />);

      await screen.findByRole('button', { name: 'System' });
    });

    it('uses a custom title if location is matched', async () => {
      asMock(useLocation).mockReturnValue({ pathname: '/system/overview' } as Location);
      render(<SystemMenu />);

      await screen.findByRole('button', { name: 'System / Overview' });
    });

    it('uses a custom title for a plugin route if location is matched', async () => {
      asMock(useLocation).mockReturnValue({ pathname: '/system/licenses' } as Location);
      render(<SystemMenu />);

      await screen.findByRole('button', { name: 'System / Licenses' });
    });
  });
});
