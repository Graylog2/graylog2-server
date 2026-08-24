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
import userEvent from '@testing-library/user-event';
import { defaultUser } from 'defaultMockValues';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import asMock from 'helpers/mocking/AsMock';
import useCurrentUser from 'hooks/useCurrentUser';
import useSearchConfiguration from 'hooks/useSearchConfiguration';
import usePluggableLicenseCheck from 'hooks/usePluggableLicenseCheck';
import StreamsContext from 'contexts/StreamsContext';
import useCreateSearch from 'views/hooks/useCreateSearch';
import type View from 'views/logic/views/View';
import useViewsPlugin from 'views/test/testViewsPlugin';
import type { Stream } from 'logic/streams/types';
import Store from 'logic/local-storage/Store';
import type { WelcomePageMetricsPlugin, WelcomeGeneralPageMetricsPlugin } from 'components/welcome/types';

import WelcomeMetricsSection from './WelcomeMetricsSection';

jest.mock('hooks/useCurrentUser');
jest.mock('hooks/useSearchConfiguration');
jest.mock('hooks/usePluggableLicenseCheck');
jest.mock('views/hooks/useCreateSearch');
jest.mock('logic/local-storage/Store', () => ({
  get: jest.fn(),
  set: jest.fn(),
}));

const accessibleStream = { id: 'stream-id-1', title: 'Test Stream' } as Stream;

const licenseCheck = (valid: boolean): ReturnType<typeof usePluggableLicenseCheck> => ({
  data: { valid, expired: false, violated: false },
  isInitialLoading: false,
  refetch: () => {},
});

const renderWithStreams = (streams: Array<Stream>) =>
  render(
    <StreamsContext.Provider value={streams}>
      <WelcomeMetricsSection />
    </StreamsContext.Provider>,
  );

describe('WelcomeMetrics', () => {
  useViewsPlugin();

  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useCurrentUser).mockReturnValue(defaultUser);
    asMock(useCreateSearch).mockImplementation((viewPromise: Promise<View>) => viewPromise);
    asMock(useSearchConfiguration).mockReturnValue({ config: undefined, refresh: () => {}, isInitialLoading: false });
    asMock(Store.get).mockReturnValue(undefined);
    asMock(usePluggableLicenseCheck).mockReturnValue(licenseCheck(false));
  });

  it('shows a message instead of any widgets when the user has no access to any stream', async () => {
    renderWithStreams([]);

    await screen.findByText('Once you have access to a stream, your message metrics will show up here.');
    expect(screen.queryByText('Messages Today')).not.toBeInTheDocument();
    expect(useCreateSearch).not.toHaveBeenCalled();
  });

  it('allows dismissing the message shown when the user has no access to any stream, persisting the choice', async () => {
    renderWithStreams([]);

    const alert = await screen.findByText('Once you have access to a stream, your message metrics will show up here.');
    const dismissButton = await screen.findByRole('button', { name: /close alert/i });

    await userEvent.click(dismissButton);

    expect(alert).not.toBeInTheDocument();
    expect(Store.set).toHaveBeenCalledWith('welcome-metrics-no-stream-access-dismissed', true);
  });

  it('does not show the message again if it was already dismissed', async () => {
    asMock(Store.get).mockReturnValue(true);

    renderWithStreams([]);

    expect(screen.queryByText('Once you have access to a stream, your message metrics will show up here.')).toBeNull();
  });

  it('shows the default metrics view without a segmented control when no welcome page metrics plugin is registered', async () => {
    renderWithStreams([accessibleStream]);

    await screen.findByText('Messages Today');
    expect(screen.queryAllByRole('radio')).toHaveLength(0);
  });

  describe('welcome page metrics plugins', () => {
    let manifest: PluginManifest;

    const registerPlugins = (
      welcomePageMetrics: Array<WelcomePageMetricsPlugin> = [],
      welcomePageMetricsGeneral: Array<WelcomeGeneralPageMetricsPlugin> = [],
    ) => {
      manifest = new PluginManifest(
        { name: 'test-welcome-page-metrics-plugin' },
        { welcomePageMetrics, 'welcomePageMetrics.general': welcomePageMetricsGeneral },
      );
      PluginStore.register(manifest);
    };

    afterEach(() => {
      if (manifest) {
        PluginStore.unregister(manifest);
        manifest = undefined;
      }
    });

    it('shows the default metrics view when the registered plugin is not enabled', async () => {
      registerPlugins([
        { label: 'Extra', component: () => <div>Extra tab content</div>, isEnabled: () => false },
      ]);
      asMock(usePluggableLicenseCheck).mockReturnValue(licenseCheck(true));

      renderWithStreams([accessibleStream]);

      await screen.findByText('Messages Today');
      expect(screen.queryAllByRole('radio')).toHaveLength(0);
    });

    it('shows the default metrics view when the license is invalid, even though a plugin is registered', async () => {
      registerPlugins([
        {
          label: 'Extra',
          component: () => <div>Extra tab content</div>,
          isEnabled: ({ isValidSecurityLicense }) => isValidSecurityLicense,
        },
      ]);

      renderWithStreams([accessibleStream]);

      await screen.findByText('Messages Today');
      expect(screen.queryAllByRole('radio')).toHaveLength(0);
    });

    it('shows a segmented control with a General tab and the extra tab, selecting the extra tab by default', async () => {
      registerPlugins([
        {
          label: 'Extra',
          component: () => <div>Extra tab content</div>,
          isEnabled: ({ isValidSecurityLicense }) => isValidSecurityLicense,
        },
      ]);
      asMock(usePluggableLicenseCheck).mockReturnValue(licenseCheck(true));

      renderWithStreams([accessibleStream]);

      await screen.findByText('Extra tab content');
      expect(screen.getByText('General')).toBeInTheDocument();
      expect(screen.getByText('Extra')).toBeInTheDocument();
    });

    it('shows the default MetricsSearchPage content in the General tab when no welcomePageMetrics.general plugin is registered', async () => {
      registerPlugins([
        {
          label: 'Extra',
          component: () => <div>Extra tab content</div>,
          isEnabled: ({ isValidSecurityLicense }) => isValidSecurityLicense,
        },
      ]);
      asMock(usePluggableLicenseCheck).mockReturnValue(licenseCheck(true));

      renderWithStreams([accessibleStream]);

      const generalTab = await screen.findByText('General');
      await userEvent.click(generalTab);

      await screen.findByText('Messages Today');
    });

    it('shows the welcomePageMetrics.general plugin content in the General tab instead of the default MetricsSearchPage', async () => {
      registerPlugins(
        [
          {
            label: 'Extra',
            component: () => <div>Extra tab content</div>,
            isEnabled: ({ isValidSecurityLicense }) => isValidSecurityLicense,
          },
        ],
        [
          {
            component: () => <div>General metrics content</div>,
            isEnabled: ({ isValidSecurityLicense }) => isValidSecurityLicense,
          },
        ],
      );
      asMock(usePluggableLicenseCheck).mockReturnValue(licenseCheck(true));

      renderWithStreams([accessibleStream]);

      const generalTab = await screen.findByText('General');
      await userEvent.click(generalTab);

      await screen.findByText('General metrics content');
      expect(screen.queryByText('Messages Today')).not.toBeInTheDocument();
    });

    it('ignores a registered welcomePageMetrics.general plugin when there is no active welcomePageMetrics plugin', async () => {
      registerPlugins(
        [],
        [
          {
            component: () => <div>General metrics content</div>,
            isEnabled: ({ isValidSecurityLicense }) => isValidSecurityLicense,
          },
        ],
      );
      asMock(usePluggableLicenseCheck).mockReturnValue(licenseCheck(true));

      renderWithStreams([accessibleStream]);

      await screen.findByText('Messages Today');
      expect(screen.queryByText('General metrics content')).not.toBeInTheDocument();
      expect(screen.queryAllByRole('radio')).toHaveLength(0);
    });

    it('switches between the General tab and an extra tab when clicked', async () => {
      registerPlugins([
        {
          label: 'Extra',
          component: () => <div>Extra tab content</div>,
          isEnabled: ({ isValidSecurityLicense }) => isValidSecurityLicense,
        },
      ]);
      asMock(usePluggableLicenseCheck).mockReturnValue(licenseCheck(true));

      renderWithStreams([accessibleStream]);

      await screen.findByText('Extra tab content');

      const generalTab = await screen.findByText('General');
      await userEvent.click(generalTab);
      await screen.findByText('Messages Today');

      const extraTab = await screen.findByText('Extra');
      await userEvent.click(extraTab);
      await screen.findByText('Extra tab content');
    });
  });
});
