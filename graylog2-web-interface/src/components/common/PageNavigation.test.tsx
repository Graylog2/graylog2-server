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

import PageNavigation from './PageNavigation';

describe('PageNavigation', () => {
  const plugin = {
    metadata: { name: 'DummyPlugin' },
    exports: {
      pageNavigation: [
        {
          description: 'Dummy Page',
          children: [
            { description: 'First', path: '/first' },
            { description: 'Second', path: '/second' },
            { description: 'Third', path: '/third' },
          ],
        },
        {
          description: 'Dummy Page',
          children: [{ description: 'Positioned A', path: '/positioned-a', position: { after: 'Second' } }],
        },
        {
          description: 'Dummy Page',
          children: [{ description: 'Positioned B', path: '/positioned-b', position: { after: 'Second' } }],
        },
        {
          description: 'Chained Page',
          children: [
            { description: 'First', path: '/first' },
            { description: 'Second', path: '/second' },
          ],
        },
        {
          description: 'Chained Page',
          children: [{ description: 'Chained B', path: '/chained-b', position: { after: 'Chained A' } }],
        },
        {
          description: 'Chained Page',
          children: [{ description: 'Chained A', path: '/chained-a', position: { after: 'First' } }],
        },
      ],
    } as PluginExports,
  };

  beforeEach(() => {
    PluginStore.register(plugin);
  });

  afterEach(() => {
    PluginStore.unregister(plugin);
  });

  it('keeps the order of items positioned after the same item', async () => {
    render(<PageNavigation page="Dummy Page" />);

    await screen.findByRole('link', { name: 'First' });

    expect(screen.getAllByRole('link').map((link) => link.textContent)).toEqual([
      'First',
      'Second',
      'Positioned B',
      'Positioned A',
      'Third',
    ]);
  });

  it('renders an item after another positioned item which is registered later', async () => {
    render(<PageNavigation page="Chained Page" />);

    await screen.findByRole('link', { name: 'First' });

    expect(screen.getAllByRole('link').map((link) => link.textContent)).toEqual([
      'First',
      'Chained A',
      'Chained B',
      'Second',
    ]);
  });
});
