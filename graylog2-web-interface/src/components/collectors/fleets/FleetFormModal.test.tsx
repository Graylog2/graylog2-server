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
import { render, screen, waitFor, fireEvent } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import { asMock } from 'helpers/mocking';
import useSendCollectorsTelemetry from 'components/collectors/hooks/useSendCollectorsTelemetry';

import FleetFormModal from './FleetFormModal';

jest.mock('components/collectors/hooks/useSendCollectorsTelemetry');

beforeEach(() => {
  asMock(useSendCollectorsTelemetry).mockReturnValue(jest.fn());
});

describe('FleetFormModal (form behavior)', () => {
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

describe('FleetFormModal telemetry (create path)', () => {
  const sendTelemetry = jest.fn();

  beforeEach(() => {
    asMock(useSendCollectorsTelemetry).mockReturnValue(sendTelemetry);
    sendTelemetry.mockClear();
  });

  it('emits FLEET.CREATE_CANCELLED with dirty=false when user cancels without touching anything', async () => {
    const onClose = jest.fn();
    render(<FleetFormModal onClose={onClose} onSave={jest.fn()} />);
    const cancelButton = screen.getByRole('button', { name: /Cancel/i });
    await userEvent.click(cancelButton);

    expect(sendTelemetry).toHaveBeenCalledWith(
      'Fleet Create Cancelled',
      expect.objectContaining({
        app_action_value: 'fleet-create-cancel',
        dirty: false,
        fields_touched: [],
      }),
    );
    expect(onClose).toHaveBeenCalled();
  });

  it('emits FLEET.CREATE_CANCELLED with dirty=true and fields_touched after typing and blurring', async () => {
    render(<FleetFormModal onClose={jest.fn()} onSave={jest.fn()} />);

    const nameInput = screen.getByLabelText(/Name/i);
    await userEvent.type(nameInput, 'web');
    fireEvent.blur(nameInput);

    await userEvent.click(screen.getByRole('button', { name: /Cancel/i }));

    expect(sendTelemetry).toHaveBeenCalledWith(
      'Fleet Create Cancelled',
      expect.objectContaining({
        dirty: true,
        fields_touched: expect.arrayContaining(['name']),
      }),
    );
  });

  it('emits FLEET.CREATED after successful save on create path', async () => {
    const onSave = jest.fn().mockResolvedValue({ id: 'new-fleet-id' });
    const onClose = jest.fn();
    render(<FleetFormModal onClose={onClose} onSave={onSave} />);

    const nameInput = screen.getByLabelText(/Name/i) as HTMLInputElement;
    await userEvent.type(nameInput, 'web');

    const submitButton = await screen.findByRole('button', { name: /Create fleet/i });
    await waitFor(() => {
      expect(submitButton).not.toBeDisabled();
    });

    // userEvent.click on this submit button does not reliably fire Formik's onSubmit
    // under jsdom (Mantine Modal portal + pointer-events interaction). fireEvent.click
    // dispatches the click directly and triggers form submission as intended.
    // eslint-disable-next-line testing-library/prefer-user-event
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(onSave).toHaveBeenCalled();
    });

    expect(sendTelemetry).toHaveBeenCalledWith(
      'Fleet Created',
      expect.objectContaining({ app_action_value: 'fleet-create-submit', fleet_id: 'new-fleet-id' }),
    );
  });

  it('tolerates missing id in save result — emits fleet_id: ""', async () => {
    const onSave = jest.fn().mockResolvedValue(undefined);
    const onClose = jest.fn();
    render(<FleetFormModal onClose={onClose} onSave={onSave} />);

    const nameInput = screen.getByLabelText(/Name/i) as HTMLInputElement;
    await userEvent.type(nameInput, 'web');

    const submitButton = await screen.findByRole('button', { name: /Create fleet/i });
    await waitFor(() => {
      expect(submitButton).not.toBeDisabled();
    });

    // See note above: fireEvent.click needed for Formik submit under Mantine Modal.
    // eslint-disable-next-line testing-library/prefer-user-event
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(onSave).toHaveBeenCalled();
    });

    expect(sendTelemetry).toHaveBeenCalledWith('Fleet Created', expect.objectContaining({ fleet_id: '' }));
  });

  it('does NOT emit create telemetry in edit mode', async () => {
    const fleet = { id: 'f-1', name: 'web', description: '', target_version: '', created_at: '', updated_at: '' };
    render(<FleetFormModal fleet={fleet as never} onClose={jest.fn()} onSave={jest.fn()} />);
    await userEvent.click(screen.getByRole('button', { name: /Cancel/i }));

    expect(sendTelemetry).not.toHaveBeenCalledWith('Fleet Create Cancelled', expect.anything());
  });
});
