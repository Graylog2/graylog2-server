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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import MockStore from 'helpers/mocking/StoreMock';
import asMock from 'helpers/mocking/AsMock';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

import PasswordComplexityConfig from './PasswordComplexityConfig';

const mockConfig = {
  min_length: 8,
  require_uppercase: false,
  require_lowercase: true,
  require_numbers: true,
  require_special_chars: false,
};

const mockUpdate = jest.fn().mockResolvedValue(undefined);
const mockListPasswordComplexityConfig = jest.fn().mockResolvedValue(undefined);

jest.mock('logic/telemetry/useSendTelemetry');
jest.mock('stores/configurations/ConfigurationsStore', () => ({
  ConfigurationsStore: MockStore([
    'getInitialState',
    () => ({
      configuration: {
        'org.graylog2.users.PasswordComplexityConfig': mockConfig,
      },
    }),
  ]),
  ConfigurationsActions: {
    listPasswordComplexityConfig: (...args: unknown[]) => mockListPasswordComplexityConfig(...args),
    update: (...args: unknown[]) => mockUpdate(...args),
  },
}));

describe('PasswordComplexityConfig', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useSendTelemetry).mockReturnValue(jest.fn());
  });

  it('renders current password requirements', async () => {
    render(<PasswordComplexityConfig />);

    expect(await screen.findByRole('definition', { name: /require lowercase letters:/i })).toHaveTextContent(
      /Required/i,
    );
  });

  it('saves updated configuration', async () => {
    render(<PasswordComplexityConfig />);

    const editButton = await screen.findByRole('button', { name: /edit configuration/i });

    await userEvent.click(editButton);

    const minLengthInput = await screen.findByLabelText(/minimum length/i, { selector: 'input' });

    await userEvent.clear(minLengthInput);
    await userEvent.type(minLengthInput, '12');
    await userEvent.click(await screen.findByLabelText(/require uppercase letters/i, { selector: 'input' }));
    await userEvent.click(await screen.findByLabelText(/require special characters/i, { selector: 'input' }));

    await userEvent.click(await screen.findByRole('button', { name: /update configuration/i }));

    await waitFor(() => {
      expect(mockUpdate).toHaveBeenCalledWith('org.graylog2.users.PasswordComplexityConfig', {
        min_length: 12,
        require_uppercase: true,
        require_lowercase: true,
        require_numbers: true,
        require_special_chars: true,
      });
    });
  });
});
