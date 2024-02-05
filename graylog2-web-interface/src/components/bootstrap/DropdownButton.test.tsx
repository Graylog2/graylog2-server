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

import MenuItem from 'components/bootstrap/MenuItem';

import DropdownButton from './DropdownButton';

describe('DropdownButton', () => {
  it('opens upon click on trigger element', async () => {
    render((
      <DropdownButton title="Click me!">
        <MenuItem>Hey there!</MenuItem>
      </DropdownButton>
    ));

    const button = await screen.findByRole('button', { name: 'Click me!' });
    await userEvent.click(button);

    await screen.findByRole('menuitem', { name: 'Hey there!' });
  });

  it('click on menu item triggers onClick and closes menu', async () => {
    const onClick = jest.fn();

    render((
      <DropdownButton title="Click me!">
        <MenuItem onClick={onClick}>Hey there!</MenuItem>
      </DropdownButton>
    ));

    const button = await screen.findByRole('button', { name: 'Click me!' });
    await userEvent.click(button);

    const menuitem = await screen.findByRole('menuitem', { name: 'Hey there!' });
    await userEvent.click(menuitem);

    await waitFor(() => {
      expect(onClick).toHaveBeenCalled();
    });

    await waitFor(() => {
      expect(screen.queryByText('Hey there!')).not.toBeInTheDocument();
    });
  });
});
