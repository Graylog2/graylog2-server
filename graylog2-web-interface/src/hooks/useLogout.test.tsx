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
import 'whatwg-fetch';
import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import DefaultProviders from 'DefaultProviders';
import userEvent from '@testing-library/user-event';

import Routes from 'routing/Routes';
import { usePluginExports } from 'views/test/testPlugins';
import suppressConsole from 'helpers/suppressConsole';

import useLogout from './useLogout';

const TestComponent = () => {
  const logout = useLogout();

  return (
    <span>Logged in <button type="button" onClick={logout}>logout</button></span>
  );
};

const routes = [
  { path: Routes.STARTPAGE, element: <span>Logged out</span> },
  { path: '/loggedin', element: <TestComponent /> },
];
const Wrapper = () => (
  <RouterProvider router={createMemoryRouter(routes, { initialEntries: ['/loggedin'], initialIndex: 0 })} />
);

describe('useLogout', () => {
  describe('with no logout hooks', () => {
    it('works when no logout hooks are defined', async () => {
      render(<Wrapper />, { wrapper: DefaultProviders });
      await screen.findByText('Logged in');

      const logoutButton = await screen.findByRole('button', { name: 'logout' });
      userEvent.click(logoutButton);

      await screen.findByText('Logged out');
    });
  });

  describe('with logout hooks', () => {
    const logoutHook = jest.fn();
    usePluginExports({ 'hooks.logout': [logoutHook] });

    it('executes logout hook if defined', async () => {
      render(<Wrapper />, { wrapper: DefaultProviders });
      await screen.findByText('Logged in');

      const logoutButton = await screen.findByRole('button', { name: 'logout' });
      userEvent.click(logoutButton);

      await screen.findByText('Logged out');

      expect(logoutHook).toHaveBeenCalled();
    });
  });

  describe('with faulty logout hook', () => {
    const logoutHook = jest.fn();
    const faultyLogoutHook = jest.fn(() => { throw Error('Foo!'); });
    usePluginExports({ 'hooks.logout': [faultyLogoutHook, logoutHook] });

    it('continues other hooks and logging out if one hook is faulty', async () => {
      render(<Wrapper />, { wrapper: DefaultProviders });
      await screen.findByText('Logged in');

      const logoutButton = await screen.findByRole('button', { name: 'logout' });

      suppressConsole(() => {
        userEvent.click(logoutButton);
      });

      await screen.findByText('Logged out');

      expect(logoutHook).toHaveBeenCalled();
    });
  });
});
