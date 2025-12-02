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
import userEvent from '@testing-library/user-event';

import asMock from 'helpers/mocking/AsMock';
import suppressConsole from 'helpers/suppressConsole';
import ConfigurationsPage from 'pages/ConfigurationsPage';
import { usePluginExports } from 'views/test/testPlugins';
import ConfigurationSection from 'pages/configurations/ConfigurationSection';

const ComponentThrowingError = () => {
  throw new Error('Boom!');
};

const ComponentWorkingFine = () => <span>It is all good!</span>;

const SidecarConfig = jest.fn();

const coreConfiglets: PluginExports = {
  coreSystemConfigurations: [
    {
      name: 'General',
      props: {
        ConfigurationComponent: () => <span>General</span>,
        title: 'General',
      },
      SectionComponent: ConfigurationSection,
    },
    {
      name: 'Sidecar',
      props: {
        ConfigurationComponent: SidecarConfig,
        title: 'Sidecar',
      },
      SectionComponent: ConfigurationSection,
    },
  ],
};

describe('ConfigurationsPage', () => {
  usePluginExports(coreConfiglets);
  afterEach(() => {
    jest.resetAllMocks();
  });

  it('wraps core configuration elements with error boundary and displays error', async () => {
    asMock(SidecarConfig).mockImplementation(ComponentThrowingError);

    render(<ConfigurationsPage />);

    const sidecarNavItem = await screen.findByRole('link', {
      name: /sidecar/i,
    });

    await suppressConsole(async () => {
      await userEvent.click(sidecarNavItem);

      return screen.findByText('Boom!');
    });
  });

  it('wraps core configuration elements with error boundary and renders component', async () => {
    asMock(SidecarConfig).mockImplementation(ComponentWorkingFine);

    render(<ConfigurationsPage />);

    const sidecarNavItem = await screen.findByRole('link', {
      name: /sidecar/i,
    });

    await suppressConsole(async () => {
      await userEvent.click(sidecarNavItem);

      return screen.findByText('It is all good!');
    });
  });
});
