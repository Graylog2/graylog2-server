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

import ActionDropdown from './ActionDropdown';

describe('ActionDropdown', () => {
  it('opens menu when trigger element is clicked', async () => {
    render((
      <ActionDropdown element={<div className="my-trigger-element">Trigger!</div>}>
        <MenuItem>Foo</MenuItem>
      </ActionDropdown>
    ));

    const triggerButton = await screen.findByText('Trigger!');

    expect(screen.queryByText('Foo')).not.toBeInTheDocument();

    await userEvent.click(triggerButton);

    await screen.findByRole('menuitem', { name: 'Foo' });
  });

  it('stops event when trigger element is clicked', async () => {
    const onClick = jest.fn((e) => e.persist());

    render((
      <button type="button" onClick={onClick}>
        <ActionDropdown element={<div className="my-trigger-element">Trigger!</div>}>
          <MenuItem>Foo</MenuItem>
        </ActionDropdown>
      </button>
    ));

    const triggerButton = await screen.findByText('Trigger!');

    expect(screen.queryByText('Foo')).not.toBeInTheDocument();

    await userEvent.click(triggerButton);

    await screen.findByRole('menuitem', { name: 'Foo' });

    expect(onClick).not.toHaveBeenCalled();
  });

  it('closes menu when MenuItem is clicked', async () => {
    const onSelect = jest.fn();

    render((
      <ActionDropdown element={<div>Trigger!</div>}>
        <MenuItem onSelect={onSelect}>Foo</MenuItem>
      </ActionDropdown>
    ));

    const triggerButton = await screen.findByText('Trigger!');

    await userEvent.click(triggerButton);

    const menuItem = await screen.findByRole('menuitem', { name: 'Foo' });
    await userEvent.click(menuItem);

    await waitFor(() => {
      expect(onSelect).toHaveBeenCalled();
    });

    await waitFor(() => {
      expect(screen.queryByRole('menuitem', { name: 'Foo' })).not.toBeInTheDocument();
    });
  });

  it('closes menu when MenuItem with a parent element is clicked', async () => {
    const onSelect = jest.fn();

    render((
      <ActionDropdown element={<div>Trigger!</div>}>
        <div>
          <MenuItem onSelect={onSelect}>Foo</MenuItem>
        </div>
      </ActionDropdown>
    ));

    const triggerButton = await screen.findByText('Trigger!');

    await userEvent.click(triggerButton);

    const menuItem = await screen.findByRole('menuitem', { name: 'Foo' });
    await userEvent.click(menuItem);

    await waitFor(() => {
      expect(onSelect).toHaveBeenCalled();
    });

    await waitFor(() => {
      expect(screen.queryByRole('menuitem', { name: 'Foo' })).not.toBeInTheDocument();
    });
  });

  it('stops click event when MenuItem is clicked', async () => {
    const onClick = jest.fn();
    const onSelect = jest.fn();

    render((
      <button type="button" onClick={onClick}>
        <ActionDropdown element={<div>Trigger!</div>}>
          <MenuItem onSelect={onSelect}>Foo</MenuItem>
        </ActionDropdown>
      </button>
    ));

    const triggerButton = await screen.findByText('Trigger!');

    await userEvent.click(triggerButton);

    const menuItem = await screen.findByRole('menuitem', { name: 'Foo' });
    await userEvent.click(menuItem);

    await waitFor(() => {
      expect(onSelect).toHaveBeenCalled();
    });

    await waitFor(() => {
      expect(screen.queryByRole('menuitem', { name: 'Foo' })).not.toBeInTheDocument();
    });

    expect(onClick).not.toHaveBeenCalled();
  });
});
