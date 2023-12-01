import * as React from 'react';
import { render, screen, waitFor } from 'wrappedTestingLibrary';

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
    button.click();

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
    button.click();

    const menuitem = await screen.findByRole('menuitem', { name: 'Hey there!' });
    menuitem.click();

    await waitFor(() => {
      expect(onClick).toHaveBeenCalled();
    });

    await waitFor(() => {
      expect(screen.queryByText('Hey there!')).not.toBeInTheDocument();
    });
  });
});
