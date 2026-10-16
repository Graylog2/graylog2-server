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
import type { PluginExports } from 'graylog-web-plugin/plugin';
import { PluginStore } from 'graylog-web-plugin/plugin';
import { defaultUser } from 'defaultMockValues';

import { asMock } from 'helpers/mocking';
import useCurrentUser from 'hooks/useCurrentUser';

import PageNavigation from './PageNavigation';

jest.mock('hooks/useCurrentUser');

const plugin = {
  exports: {
    pageNavigation: [
      {
        description: 'Test Page',
        children: [
          { description: 'Overview', path: '/overview' },
          { description: 'Definitions', path: '/definitions' },
          { description: 'Notifications', path: '/notifications' },
        ],
      },
      {
        description: 'Test Page',
        children: [{ description: 'Procedures', path: '/procedures', position: { after: 'Definitions' } }],
      },
      {
        description: 'Test Page',
        children: [{ description: 'Chains', path: '/chains', position: { after: 'Definitions' } }],
      },
    ],
  } as PluginExports,
  metadata: { name: 'PageNavigation test plugin' },
};

describe('PageNavigation', () => {
  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(defaultUser);
    PluginStore.register(plugin);
  });

  afterEach(() => {
    PluginStore.unregister(plugin);
  });

  it('keeps the order of items positioned after the same item when merging several plugin entries', async () => {
    render(<PageNavigation page="Test Page" />);

    await screen.findByRole('link', { name: 'Overview' });

    expect(screen.getAllByRole('link').map((link) => link.textContent)).toEqual([
      'Overview',
      'Definitions',
      'Chains',
      'Procedures',
      'Notifications',
    ]);
  });
});
