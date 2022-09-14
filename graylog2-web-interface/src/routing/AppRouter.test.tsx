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
import { renderUnwrapped as render, screen } from 'wrappedTestingLibrary';
import DefaultProviders from 'DefaultProviders';
import { MemoryRouter } from 'react-router-dom';
import { defaultUser } from 'defaultMockValues';

import CurrentUserContext from 'contexts/CurrentUserContext';
import mockComponent from 'helpers/mocking/MockComponent';
import asMock from 'helpers/mocking/AsMock';
import usePluginEntities from 'hooks/usePluginEntities';
import AppConfig from 'util/AppConfig';

import AppRouter from './AppRouter';

jest.mock('components/throughput/GlobalThroughput', () => mockComponent('GlobalThroughput'));
jest.mock('components/layout/Footer', () => mockComponent('Footer'));

// To prevent exceptions from getting swallowed
jest.mock('components/errors/RouterErrorBoundary', () => mockComponent('RouterErrorBoundary'));

jest.mock('pages/StartPage', () => () => <>This is the start page</>);
jest.mock('hooks/usePluginEntities');

jest.mock('util/AppConfig', () => ({
  gl2AppPathPrefix: jest.fn(() => ''),
  gl2ServerUrl: jest.fn(() => undefined),
  gl2DevMode: jest.fn(() => false),
  isFeatureEnabled: jest.fn(() => false),
  isCloud: jest.fn(() => false),
}));

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  BrowserRouter: ({ children }) => children,
}));

const AppRouterWithContext = ({ path }: { path?: string }) => (
  <DefaultProviders>
    <CurrentUserContext.Provider value={defaultUser}>
      <MemoryRouter initialEntries={[path]}>
        <AppRouter />
      </MemoryRouter>
    </CurrentUserContext.Provider>
  </DefaultProviders>
);

AppRouterWithContext.defaultProps = {
  path: '/',
};

describe('AppRouter', () => {
  beforeEach(() => {
    asMock(usePluginEntities).mockReturnValue([]);
    AppConfig.isFeatureEnabled = jest.fn(() => false);
  });

  it('routes to Getting Started Page for `/` or empty location', async () => {
    render(<AppRouterWithContext path={undefined} />);

    await screen.findByText('This is the start page');
  });

  it('renders a not found page for unknown URLs', async () => {
    render(<AppRouterWithContext path="/this-url-is-not-registered-and-should-never-be" />);

    await screen.findByText('Page not found');
  });

  describe('plugin routes', () => {
    it('renders simple plugin routes', async () => {
      asMock(usePluginEntities).mockReturnValue([{ component: () => <span>Hey there!</span>, path: '/a-plugin-route' }]);
      const { findByText } = render(<AppRouterWithContext path="/a-plugin-route" />);

      await findByText('Hey there!');
    });

    it('renders null-parent component plugin routes without application chrome', async () => {
      asMock(usePluginEntities).mockReturnValue([{ parentComponent: null, component: () => <span>Hey there!</span>, path: '/without-chrome' }]);

      const { findByText, queryByTitle } = render(<AppRouterWithContext path="/without-chrome" />);

      await findByText('Hey there!');

      expect(queryByTitle('Graylog Logo')).toBeNull();
    });

    it('does not render plugin route when required feature flag is not enabled', async () => {
      asMock(usePluginEntities).mockReturnValue([{ component: () => <span>Hey there!</span>, path: '/a-plugin-route', requiredFeatureFlag: 'a_feature_flag' }]);
      render(<AppRouterWithContext path="/a-plugin-route" />);

      await screen.findByText('Page not found');

      expect(screen.queryByText('Hey there!')).not.toBeInTheDocument();
    });

    it('render plugin route when required feature flag is enabled', async () => {
      asMock(AppConfig.isFeatureEnabled).mockReturnValue(true);
      asMock(usePluginEntities).mockReturnValue([{ component: () => <span>Hey there!</span>, path: '/a-plugin-route', requiredFeatureFlag: 'a_feature_flag' }]);
      const { findByText } = render(<AppRouterWithContext path="/a-plugin-route" />);

      await findByText('Hey there!');
    });
  });
});
