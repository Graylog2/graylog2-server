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

import WelcomePageConfig from './WelcomePageConfig';

const mockConfig = { disable_metrics: false };

const mockList = jest.fn().mockResolvedValue(undefined);
const mockUpdate = jest.fn().mockResolvedValue(undefined);

jest.mock('logic/telemetry/useSendTelemetry');
jest.mock('stores/configurations/ConfigurationsStore', () => ({
  ConfigurationsStore: MockStore([
    'getInitialState',
    () => ({
      configuration: {
        'org.graylog2.configuration.WelcomePageConfiguration': mockConfig,
      },
    }),
  ]),
  ConfigurationsActions: {
    list: (...args: unknown[]) => mockList(...args),
    update: (...args: unknown[]) => mockUpdate(...args),
  },
}));

describe('WelcomePageConfig', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useSendTelemetry).mockReturnValue(jest.fn());
  });

  it('renders current configuration', async () => {
    render(<WelcomePageConfig />);

    await screen.findByText('Welcome page metrics:');
    await screen.findByText('Enabled');
  });

  it('saves updated configuration', async () => {
    render(<WelcomePageConfig />);

    const editButton = await screen.findByRole('button', { name: /edit configuration/i });

    await userEvent.click(editButton);

    await userEvent.click(await screen.findByLabelText(/enable welcome page metrics/i, { selector: 'input' }));

    await userEvent.click(await screen.findByRole('button', { name: /update configuration/i }));

    await waitFor(() => {
      expect(mockUpdate).toHaveBeenCalledWith('org.graylog2.configuration.WelcomePageConfiguration', {
        disable_metrics: true,
      });
    });
  });
});
