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

import Menu from 'components/bootstrap/Menu';
import { MenuItem } from 'components/bootstrap';

import useClickToOpenMenu, { MenuAnchor } from './useClickToOpenMenu';

const TestTrigger = ({ onSelect = () => {} }: { onSelect?: () => void }) => {
  const { triggerRef, opened, onOpenChange, anchorPosition, onClick, onKeyDown } =
    useClickToOpenMenu<HTMLButtonElement>();

  return (
    <Menu opened={opened} onChange={onOpenChange} withinPortal position="bottom-start">
      <Menu.Target>
        <MenuAnchor style={{ left: anchorPosition?.x ?? 0, top: anchorPosition?.y ?? 0 }} />
      </Menu.Target>
      <button type="button" ref={triggerRef} onClick={onClick} onKeyDown={onKeyDown}>
        Trigger
      </button>
      <Menu.Dropdown>
        <MenuItem onClick={onSelect}>Item</MenuItem>
      </Menu.Dropdown>
    </Menu>
  );
};

describe('useClickToOpenMenu', () => {
  it('should open the menu when clicking the trigger', async () => {
    render(<TestTrigger />);

    await userEvent.click(await screen.findByRole('button', { name: /trigger/i }));

    await screen.findByRole('menuitem', { name: /item/i });
  });

  it('should open the menu with the Enter key', async () => {
    render(<TestTrigger />);

    const trigger = await screen.findByRole('button', { name: /trigger/i });
    trigger.focus();

    await userEvent.keyboard('{Enter}');

    await screen.findByRole('menuitem', { name: /item/i });
  });

  it('should return focus to the trigger once the menu closes', async () => {
    const onSelect = jest.fn();
    render(<TestTrigger onSelect={onSelect} />);

    const trigger = await screen.findByRole('button', { name: /trigger/i });
    await userEvent.click(trigger);
    await userEvent.click(await screen.findByRole('menuitem', { name: /item/i }));

    expect(onSelect).toHaveBeenCalledTimes(1);
    await waitFor(() => expect(trigger).toHaveFocus());
  });
});
