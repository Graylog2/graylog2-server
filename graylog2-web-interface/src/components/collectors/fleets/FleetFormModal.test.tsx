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
import userEvent from '@testing-library/user-event';

import FleetFormModal from './FleetFormModal';

describe('FleetFormModal', () => {
  it('renders name input field', async () => {
    render(<FleetFormModal onClose={jest.fn()} onSave={jest.fn()} />);

    await screen.findByLabelText(/name/i);
  });

  it('renders description input field', async () => {
    render(<FleetFormModal onClose={jest.fn()} onSave={jest.fn()} />);

    await screen.findByLabelText(/description/i);
  });

  it('calls onClose when cancel clicked', async () => {
    const onClose = jest.fn();
    render(<FleetFormModal onClose={onClose} onSave={jest.fn()} />);

    const cancelButton = await screen.findByRole('button', { name: /cancel/i });
    await userEvent.click(cancelButton);

    expect(onClose).toHaveBeenCalled();
  });

  it('disables save button when name is empty', async () => {
    render(<FleetFormModal onClose={jest.fn()} onSave={jest.fn()} />);

    const saveButton = await screen.findByRole('button', { name: /create fleet/i });

    expect(saveButton).toBeDisabled();
  });
});
