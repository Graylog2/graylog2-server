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

const PAGE = 'Test Page';
const overview = { description: 'Overview', path: '/overview' };
const definitions = { description: 'Definitions', path: '/definitions' };
const notifications = { description: 'Notifications', path: '/notifications' };
const procedures = { description: 'Procedures', path: '/procedures', position: { after: 'Definitions' } };
const chains = { description: 'Chains', path: '/chains', position: { after: 'Definitions' } };

const createPlugin = (entries: Array<Array<{ description: string; path: string }>>) => ({
  exports: {
    pageNavigation: entries.map((children) => ({ description: PAGE, children })),
  } as PluginExports,
  metadata: { name: 'PageNavigation test plugin' },
});

describe('PageNavigation', () => {
  let plugin: ReturnType<typeof createPlugin>;

  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(defaultUser);
  });

  afterEach(() => {
    PluginStore.unregister(plugin);
  });

  it.each([
    ['a single plugin entry', [[overview, definitions, notifications, procedures, chains]]],
    ['three plugin entries', [[overview, definitions, notifications], [procedures], [chains]]],
    ['four plugin entries', [[overview, definitions], [notifications], [procedures], [chains]]],
  ])('keeps the registration order of items positioned after the same item with %s', async (_name, entries) => {
    plugin = createPlugin(entries);
    PluginStore.register(plugin);

    render(<PageNavigation page={PAGE} />);

    await screen.findByRole('link', { name: 'Overview' });

    expect(screen.getAllByRole('link').map((link) => link.textContent)).toEqual([
      'Overview',
      'Definitions',
      'Procedures',
      'Chains',
      'Notifications',
    ]);
  });
});
