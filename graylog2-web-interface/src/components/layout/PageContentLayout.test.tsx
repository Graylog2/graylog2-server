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
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import PageContentLayout from './PageContentLayout';

jest.mock('injection/StoreProvider', () => ({
  getStore: () => ({
    getInitialState: () => ({
      system: { version: '23.42.0-SNAPSHOT+SPECIALFEATURE', hostname: 'hopper.local' },
    }),
    jvm: jest.fn(() => Promise.resolve({ info: 'SomeJDK v12.0.0' })),
    listen: jest.fn(() => () => {}),
  }),
}));

describe('PageContentLayout', () => {
  it('renders its children', async () => {
    render(<PageContentLayout><div>The content</div></PageContentLayout>);

    await screen.findByText('Graylog 23.42.0-SNAPSHOT+SPECIALFEATURE', { exact: false });

    expect(screen.getByText('The content')).not.toBeNull();
  });

  it('displays global notifications', async () => {
    PluginStore.register(new PluginManifest({}, {
      globalNotifications: [
        {
          key: 'org.graylog.plugins.globalNotification.licenseWarning',
          component: () => <>Your license is expiring.</>,
        },
      ],
    }));

    render(<PageContentLayout><div>The content</div></PageContentLayout>);

    await screen.findByText('Graylog 23.42.0-SNAPSHOT+SPECIALFEATURE', { exact: false });

    expect(screen.getByText('Your license is expiring.')).not.toBeNull();
  });
});
