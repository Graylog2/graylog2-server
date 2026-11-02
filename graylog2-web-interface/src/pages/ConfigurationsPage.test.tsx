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
import { render, renderUnwrapped, screen } from 'wrappedTestingLibrary';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import DefaultProviders from 'DefaultProviders';
import type { PluginExports } from 'graylog-web-plugin/plugin';
import userEvent from '@testing-library/user-event';
import { memoryRouterFuture } from 'reactRouterFutureFlags';

import asMock from 'helpers/mocking/AsMock';
import suppressConsole from 'helpers/suppressConsole';
import useLocation from 'routing/useLocation';
import ConfigurationsPage from 'pages/ConfigurationsPage';
import { usePluginExports } from 'views/test/testPlugins';
import ConfigurationSection from 'pages/configurations/ConfigurationSection';
import useRuntimeTunables from 'components/configurations/runtime-tunables/useRuntimeTunables';
import useRuntimeTunableMutations from 'components/configurations/runtime-tunables/useRuntimeTunableMutations';
import useFeatureTunables from 'components/configurations/runtime-tunables/useFeatureTunables';
import {
  passwordFeature,
  passwordFeatureSummary,
} from 'components/configurations/runtime-tunables/__tests__/fixtures';

jest.mock('components/configurations/runtime-tunables/useRuntimeTunables');
jest.mock('components/configurations/runtime-tunables/useRuntimeTunableMutations');
jest.mock('components/configurations/runtime-tunables/useFeatureTunables');
jest.mock('logic/telemetry/useSendTelemetry', () => () => jest.fn());

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
  beforeEach(() => {
    asMock(useRuntimeTunables).mockReturnValue({ features: [], isLoading: false, isError: false });
  });
  afterEach(() => {
    jest.resetAllMocks();
  });

  it('redirects unknown sections to the first section', async () => {
    asMock(SidecarConfig).mockImplementation(ComponentWorkingFine);
    const CurrentPath = () => <span>Path: {useLocation().pathname}</span>;

    renderUnwrapped(
      <MemoryRouter
        initialEntries={['/system/configurations/RemovedSection']}
        future={memoryRouterFuture}>
        <Routes>
          <Route path="/system/configurations/*" element={<ConfigurationsPage />} />
        </Routes>
        <CurrentPath />
      </MemoryRouter>,
      { wrapper: DefaultProviders },
    );

    await screen.findByText('Path: /system/configurations/General');
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

const HandBuiltPasswordConfig = () => <span>Hand-built password configlet</span>;

const featureConfiglets: PluginExports = {
  coreSystemConfigurations: [
    {
      name: 'Password Policy',
      feature: 'password_policy',
      props: {
        ConfigurationComponent: HandBuiltPasswordConfig,
        title: 'Password Policy',
      },
      SectionComponent: ConfigurationSection,
    },
  ],
};

describe('ConfigurationsPage with runtime tunables', () => {
  usePluginExports(featureConfiglets);
  beforeEach(() => {
    asMock(useRuntimeTunableMutations).mockReturnValue({ updateTunable: jest.fn(), resetTunable: jest.fn() });
  });

  it('renders hand-built and generated configlets in the feature section', async () => {
    asMock(useRuntimeTunables).mockReturnValue({
      features: [passwordFeatureSummary],
      isLoading: false,
      isError: false,
    });
    asMock(useFeatureTunables).mockReturnValue({ tunables: passwordFeature.tunables, isLoading: false, isError: false });

    render(<ConfigurationsPage />);
    await userEvent.click(await screen.findByRole('link', { name: /password policy/i }));

    await screen.findByText('Hand-built password configlet');
    await screen.findByRole('heading', { name: /password complexity/i });
    expect(useFeatureTunables).toHaveBeenCalledWith('password_policy', { enabled: true });
  });

  it('keeps hand-built configlets when loading tunables fails', async () => {
    asMock(useRuntimeTunables).mockReturnValue({ features: [], isLoading: false, isError: true });

    render(<ConfigurationsPage />);
    await userEvent.click(await screen.findByRole('link', { name: /password policy/i }));

    await screen.findByText('Hand-built password configlet');
  });
});
