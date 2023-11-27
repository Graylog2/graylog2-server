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
import { useContext } from 'react';
import { renderUnwrapped as render, screen } from 'wrappedTestingLibrary';
import DefaultProviders from 'DefaultProviders';
import type { RouteObject } from 'react-router-dom';
import { createBrowserRouter, createMemoryRouter } from 'react-router-dom';
import { defaultUser } from 'defaultMockValues';
import type { PluginExports } from 'graylog-web-plugin/plugin';

import CurrentUserContext from 'contexts/CurrentUserContext';
import mockComponent from 'helpers/mocking/MockComponent';
import asMock from 'helpers/mocking/AsMock';
import usePluginEntities from 'hooks/usePluginEntities';
import AppConfig from 'util/AppConfig';
import GlobalContextProviders from 'contexts/GlobalContextProviders';
import HotkeysProvider from 'contexts/HotkeysProvider';

import AppRouter from './AppRouter';

jest.mock('components/throughput/GlobalThroughput', () => mockComponent('GlobalThroughput'));
jest.mock('components/layout/Footer', () => mockComponent('Footer'));

// To prevent exceptions from getting swallowed
jest.mock('components/errors/RouterErrorBoundary', () => mockComponent('RouterErrorBoundary'));

jest.mock('pages/StartPage', () => () => <>This is the start page</>);
jest.mock('hooks/usePluginEntities');
jest.mock('contexts/GlobalContextProviders', () => jest.fn(({ children }: React.PropsWithChildren<{}>) => children));
jest.mock('hooks/useFeature', () => (featureFlag: string) => featureFlag === 'frontend_hotkeys');

jest.mock('util/AppConfig', () => ({
  gl2AppPathPrefix: jest.fn(() => ''),
  gl2ServerUrl: jest.fn(() => undefined),
  gl2DevMode: jest.fn(() => false),
  isFeatureEnabled: jest.fn(() => false),
  isCloud: jest.fn(() => false),
}));

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  createBrowserRouter: jest.fn(),
}));

const AppRouterWithContext = () => (
  <HotkeysProvider>
    <DefaultProviders>
      <CurrentUserContext.Provider value={defaultUser}>
        <AppRouter />
      </CurrentUserContext.Provider>
    </DefaultProviders>
  </HotkeysProvider>
);

AppRouterWithContext.defaultProps = {
  path: '/',
};

const setInitialPath = (path: string) => {
  asMock(createBrowserRouter).mockImplementation((routes: RouteObject[]) => createMemoryRouter(routes, {
    initialEntries: [path],
  }));
};

const mockRoutes = (routes: PluginExports['routes']) => {
  const pluginExports: PluginExports = {
    routes,
  };
  asMock(usePluginEntities).mockImplementation((key: keyof PluginExports) => pluginExports[key] ?? []);
};

describe('AppRouter', () => {
  const defaultPlugins = {
    perspectives: [
      {
        id: 'default',
        title: 'Default Perspective',
        brandComponent: () => <div />,
        brandLink: '',
      },
    ],
  };

  beforeEach(() => {
    asMock(usePluginEntities).mockImplementation((entityKey) => (defaultPlugins[entityKey] ?? []));
    AppConfig.isFeatureEnabled = jest.fn(() => false);
    asMock(createBrowserRouter).mockImplementation((routes: RouteObject[]) => createMemoryRouter(routes));
  });

  it('routes to Getting Started Page for `/` or empty location', async () => {
    render(<AppRouterWithContext />);

    await screen.findByText('This is the start page');
  });

  it('renders a not found page for unknown URLs', async () => {
    setInitialPath('/this-url-is-not-registered-and-should-never-be');
    render(<AppRouterWithContext />);

    await screen.findByText('Page not found');
  });

  describe('plugin routes', () => {
    it('renders simple plugin routes', async () => {
      mockRoutes([{ component: () => <span>Hey there!</span>, path: '/a-plugin-route' }]);
      setInitialPath('/a-plugin-route');
      const { findByText } = render(<AppRouterWithContext />);

      await findByText('Hey there!');
    });

    it('renders null-parent component plugin routes without application chrome', async () => {
      mockRoutes([{ parentComponent: null, component: () => <span>Hey there!</span>, path: '/without-chrome' }]);

      setInitialPath('/without-chrome');
      const { findByText, queryByTitle } = render(<AppRouterWithContext />);

      await findByText('Hey there!');

      expect(queryByTitle('Graylog Logo')).toBeNull();
    });

    it('does not render plugin route when required feature flag is not enabled', async () => {
      mockRoutes([{ component: () => <span>Hey there!</span>, path: '/a-plugin-route', requiredFeatureFlag: 'a_feature_flag' }]);
      setInitialPath('/a-plugin-route');
      render(<AppRouterWithContext />);

      await screen.findByText('Page not found');

      expect(screen.queryByText('Hey there!')).not.toBeInTheDocument();
    });

    it('render plugin route when required feature flag is enabled', async () => {
      asMock(AppConfig.isFeatureEnabled).mockReturnValue(true);
      mockRoutes([{ component: () => <span>Hey there!</span>, path: '/a-plugin-route', requiredFeatureFlag: 'a_feature_flag' }]);
      setInitialPath('/a-plugin-route');
      const { findByText } = render(<AppRouterWithContext />);

      await findByText('Hey there!');
    });

    it('renders null-parent component plugin wrapped in global providers', async () => {
      const TestContext = React.createContext(undefined);
      asMock(GlobalContextProviders).mockImplementation(({ children }: React.PropsWithChildren<{}>) => <TestContext.Provider value={42}>{children}</TestContext.Provider>);

      const TestComponent = () => {
        const contextValue = useContext(TestContext);

        return <span>Current context value is {contextValue}</span>;
      };

      mockRoutes([{ parentComponent: null, component: TestComponent, path: '/test' }]);

      setInitialPath('/test');
      const { findByText } = render(<AppRouterWithContext />);

      await findByText('Current context value is 42');
    });
  });
});
