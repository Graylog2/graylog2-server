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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import { MenuItem } from 'components/bootstrap';

import WidgetActionDropdown from './WidgetActionDropdown';

describe('WidgetActionDropdown', () => {
  it('opens menu when trigger element is clicked', async () => {
    render((
      <WidgetActionDropdown>
        <MenuItem>Foo</MenuItem>
      </WidgetActionDropdown>
    ));

    const menuButton = await screen.findByRole('button', { name: /open actions dropdown/i });

    expect(screen.queryByText('Foo')).not.toBeInTheDocument();

    await userEvent.click(menuButton);

    await screen.findByRole('menuitem', { name: 'Foo' });
  });

  it('closes menu when MenuItem is clicked', async () => {
    const onSelect = jest.fn();

    render((
      <WidgetActionDropdown>
        <MenuItem onSelect={onSelect}>Foo</MenuItem>
      </WidgetActionDropdown>
    ));

    const menuButton = await screen.findByRole('button', { name: /open actions dropdown/i });
    await userEvent.click(menuButton);

    const fooAction = await screen.findByRole('menuitem', { name: 'Foo' });
    await userEvent.click(fooAction);

    await waitFor(() => {
      expect(screen.queryByText('Foo')).not.toBeInTheDocument();
    });

    expect(onSelect).toHaveBeenCalled();
  });
});
