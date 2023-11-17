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

import { appPrefixed } from 'util/URLUtils';
import { asMock } from 'helpers/mocking';
import Menu from 'components/bootstrap/Menu';

import OriginalNavigationLink from './NavigationLink';

jest.mock('util/URLUtils', () => ({ appPrefixed: jest.fn((path) => path), qualifyUrl: jest.fn((path) => path) }));

const NavigationLink = (props: React.ComponentProps<typeof OriginalNavigationLink>) => (props.topLevel
  ? <OriginalNavigationLink {...props} />
  : (
    <Menu opened>
      <Menu.Dropdown>
        <OriginalNavigationLink {...props} />
      </Menu.Dropdown>
    </Menu>
  ));

describe('NavigationLink', () => {
  it('renders with simple props', async () => {
    render(<NavigationLink description="Hello there!" path="/hello" />);

    const link = await screen.findByRole('menuitem', { name: 'Hello there!' });

    expect(link).toHaveAttribute('href', '/hello');
  });

  it('does not prefix URL with app prefix', async () => {
    asMock(appPrefixed).mockImplementation((path) => `/someprefix${path}`);
    render(<NavigationLink description="Hello there!" path="/hello" />);

    const link = await screen.findByRole('menuitem', { name: 'Hello there!' });

    expect(link).toHaveAttribute('href', '/hello');
    expect(appPrefixed).not.toHaveBeenCalled();
  });

  it('renders with NavItem if toplevel', async () => {
    render(<NavigationLink description="Hello there!" path="/hello" topLevel />);

    await screen.findByRole('link', { name: /Hello there!/i });
  });
});
