import 'whatwg-fetch';
import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import DefaultProviders from 'DefaultProviders';

import Routes from 'routing/Routes';
import { usePluginExports } from 'views/test/testPlugins';

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
      logoutButton.click();

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
      logoutButton.click();

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
      logoutButton.click();

      await screen.findByText('Logged out');

      expect(logoutHook).toHaveBeenCalled();
    });
  });
});
