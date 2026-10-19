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

import asMock from 'helpers/mocking/AsMock';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import FetchError from 'logic/errors/FetchError';
import usePluginEntities from 'hooks/usePluginEntities';

import RuntimeTunableConfiglet from './RuntimeTunableConfiglet';
import useRuntimeTunableMutations from './useRuntimeTunableMutations';
import { passwordFeature } from './__tests__/fixtures';
import type { RuntimeTunable } from './types';

jest.mock('logic/telemetry/useSendTelemetry');
jest.mock('hooks/usePluginEntities');
jest.mock('./useRuntimeTunableMutations');

const tunable = passwordFeature.tunables[0];
const updateTunable = jest.fn();
const resetTunable = jest.fn();
const sendTelemetry = jest.fn();

const renderConfiglet = (overrides: Partial<RuntimeTunable> = {}) =>
  render(<RuntimeTunableConfiglet tunable={{ ...tunable, ...overrides }} featureId="password_policy" />);

describe('RuntimeTunableConfiglet', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    updateTunable.mockResolvedValue(undefined);
    resetTunable.mockResolvedValue(undefined);
    asMock(useRuntimeTunableMutations).mockReturnValue({ updateTunable, resetTunable });
    asMock(useSendTelemetry).mockReturnValue(sendTelemetry);
    asMock(usePluginEntities).mockReturnValue([]);
  });

  it('shows a summary and marks values that differ from defaults', async () => {
    renderConfiglet();

    await screen.findByRole('heading', { name: /password complexity/i });
    expect(screen.getByText('Minimum length:')).toBeInTheDocument();
    expect(screen.getByRole('definition', { name: /minimum length/i })).toHaveTextContent('8');
    expect(screen.getByRole('definition', { name: /minimum length/i })).toHaveTextContent(/modified/i);
    expect(screen.getByRole('definition', { name: /require uppercase letters/i })).toHaveTextContent('Enabled');
  });

  it('saves edited values and sends telemetry', async () => {
    renderConfiglet();

    await userEvent.click(await screen.findByRole('button', { name: /edit configuration/i }));
    const minLength = await screen.findByRole('spinbutton', { name: /minimum length/i });
    await userEvent.clear(minLength);
    await userEvent.type(minLength, '12');
    await userEvent.click(screen.getByRole('button', { name: /update configuration/i }));

    await waitFor(() =>
      expect(updateTunable).toHaveBeenCalledWith({
        id: 'users.password_complexity',
        values: { min_length: 12, require_uppercase: true },
      }),
    );
    await waitFor(() =>
      expect(sendTelemetry).toHaveBeenCalledWith('Configurations Runtime Tunable Updated', {
        app_action_value: 'configuration-save',
        tunable_id: 'users.password_complexity',
        feature_id: 'password_policy',
        changed_fields: ['min_length'],
      }),
    );
    expect(useSendTelemetry).toHaveBeenCalledWith('password_policy');
  });

  it('shows server validation errors on the fields and globally', async () => {
    updateTunable.mockRejectedValue(
      new FetchError('Bad Request', 400, { failed: true, errors: { min_length: ['too short'], _global: ['nope'] } }),
    );
    renderConfiglet();

    await userEvent.click(await screen.findByRole('button', { name: /edit configuration/i }));
    await userEvent.click(await screen.findByRole('button', { name: /update configuration/i }));

    await screen.findByText('too short');
    await screen.findByText('nope');
  });

  it('resets to defaults after confirmation', async () => {
    renderConfiglet();

    await userEvent.click(await screen.findByRole('button', { name: /edit configuration/i }));
    await userEvent.click(await screen.findByRole('button', { name: /reset to defaults/i }));
    await userEvent.click(await screen.findByRole('button', { name: /^reset$/i }));

    await waitFor(() => expect(resetTunable).toHaveBeenCalledWith('users.password_complexity'));
    await waitFor(() =>
      expect(sendTelemetry).toHaveBeenCalledWith(
        'Configurations Runtime Tunable Reset',
        expect.objectContaining({ tunable_id: 'users.password_complexity' }),
      ),
    );
  });

  it('keeps the modal open when resetting fails', async () => {
    resetTunable.mockRejectedValue(new Error('Forbidden'));
    renderConfiglet();

    await userEvent.click(await screen.findByRole('button', { name: /edit configuration/i }));
    await userEvent.click(await screen.findByRole('button', { name: /reset to defaults/i }));
    await userEvent.click(await screen.findByRole('button', { name: /^reset$/i }));

    await waitFor(() => expect(screen.queryByRole('button', { name: /^reset$/i })).not.toBeInTheDocument());
    expect(screen.getByRole('button', { name: /reset to defaults/i })).toBeInTheDocument();
    expect(sendTelemetry).not.toHaveBeenCalled();
  });

  it('hides editing without permission', async () => {
    renderConfiglet({ canEdit: false });

    await screen.findByRole('heading', { name: /password complexity/i });
    expect(screen.queryByRole('button', { name: /edit configuration/i })).not.toBeInTheDocument();
  });
});
