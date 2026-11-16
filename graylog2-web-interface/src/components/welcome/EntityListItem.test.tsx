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

import React from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import EntityListItem from 'components/welcome/EntityListItem';
import { createGRN } from 'logic/permissions/GRN';
import { prefixUrl } from 'routing/Routes';

const grn = createGRN('dashboard', '1');

describe('EntityListItem', () => {
  it('Show type', async () => {
    render(<EntityListItem grn={grn} title="Title 1" />);

    await screen.findByText('dashboard');
  });

  it('Show correct link', async () => {
    render(<EntityListItem grn={grn} title="Title 1" />);

    const link = await screen.findByText('Title 1');

    expect(link).toHaveAttribute('href', '/dashboards/1');
  });

  describe('with an entity type registered by a plugin', () => {
    const plugin = new PluginManifest(
      {},
      {
        entityTypeTitles: [{ type: 'team_users', title: 'team members' }],
        entityTypeRoute: [{ type: 'team_users', route: (id: string) => prefixUrl(`/teams/${id}`) }],
      },
    );

    beforeEach(() => {
      PluginStore.register(plugin);
    });

    afterEach(() => {
      PluginStore.unregister(plugin);
    });

    it('shows the title registered by the plugin', async () => {
      render(<EntityListItem grn={createGRN('team_users', '1')} title="Security Operations" />);

      await screen.findByText('team members');
    });
  });
});
