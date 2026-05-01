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
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import NameOnlyEntityManager from 'components/common/NameOnlyEntityManager';

const baseProps = {
  title: 'Manage Tags',
  entityLabel: 'tag',
  items: [
    { id: '1', value: 'phishing' },
    { id: '2', value: 'malware' },
  ],
};

describe('<NameOnlyEntityManager />', () => {
  it('renders items sorted alphabetically', () => {
    render(
      <NameOnlyEntityManager
        {...baseProps}
        onAdd={jest.fn()}
        onUpdate={jest.fn()}
        onDelete={jest.fn()}
      />,
    );

    const rows = screen.getAllByText(/phishing|malware/);
    expect(rows[0]).toHaveTextContent('malware');
    expect(rows[1]).toHaveTextContent('phishing');
  });

  it('calls onAdd with the new value', async () => {
    const onAdd = jest.fn().mockResolvedValue(undefined);
    render(
      <NameOnlyEntityManager
        {...baseProps}
        onAdd={onAdd}
        onUpdate={jest.fn()}
        onDelete={jest.fn()}
      />,
    );

    await userEvent.click(screen.getByTestId('add-tag'));
    await userEvent.type(screen.getByTestId('new-tag-input'), 'ransomware');
    await userEvent.click(screen.getByTestId('save-new-tag'));

    expect(onAdd).toHaveBeenCalledWith('ransomware');
  });

  it('calls onUpdate when editing', async () => {
    const onUpdate = jest.fn().mockResolvedValue(undefined);
    render(
      <NameOnlyEntityManager
        {...baseProps}
        onAdd={jest.fn()}
        onUpdate={onUpdate}
        onDelete={jest.fn()}
      />,
    );

    const editButtons = screen.getAllByTestId('edit-tag');
    await userEvent.click(editButtons[0]);

    const input = screen.getByTestId('tag-input');
    await userEvent.clear(input);
    await userEvent.type(input, 'updated');
    await userEvent.click(screen.getByTestId('save-edit-tag'));

    expect(onUpdate).toHaveBeenCalledWith(expect.any(String), 'updated');
  });

  it('confirms before deleting', async () => {
    const onDelete = jest.fn().mockResolvedValue(undefined);
    render(
      <NameOnlyEntityManager
        {...baseProps}
        onAdd={jest.fn()}
        onUpdate={jest.fn()}
        onDelete={onDelete}
      />,
    );

    const deleteButtons = screen.getAllByTestId('delete-tag');
    await userEvent.click(deleteButtons[0]);

    expect(screen.getByText(/are you sure you want to delete this tag/i)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /confirm/i }));
    expect(onDelete).toHaveBeenCalled();
  });

  it('renders custom delete warning when provided', async () => {
    render(
      <NameOnlyEntityManager
        {...baseProps}
        onAdd={jest.fn()}
        onUpdate={jest.fn()}
        onDelete={jest.fn()}
        renderDeleteWarning={(item) => <em>Removing {item.value} will affect things.</em>}
      />,
    );

    const deleteButtons = screen.getAllByTestId('delete-tag');
    await userEvent.click(deleteButtons[0]);

    expect(screen.getByText(/will affect things/i)).toBeInTheDocument();
  });
});
