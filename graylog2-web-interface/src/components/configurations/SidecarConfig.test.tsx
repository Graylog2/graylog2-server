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
import { fireEvent, render, screen, waitFor } from 'wrappedTestingLibrary';

import MockStore from 'helpers/mocking/StoreMock';

import SidecarConfig from './SidecarConfig';

const mockConfig = {
  sidecar_configuration_override: false,
  sidecar_expiration_threshold: 'P14D',
  sidecar_inactive_threshold: 'PT1M',
  sidecar_send_status: true,
  sidecar_update_interval: 'PT30S',
};

let mockUpdate;

jest.mock('stores/configurations/ConfigurationsStore', () => {
  mockUpdate = jest.fn().mockReturnValue(Promise.resolve());

  return ({
    ConfigurationsStore: MockStore(['getInitialState', () => ({
      configuration: {
        'org.graylog.plugins.sidecar.system.SidecarConfiguration': mockConfig,
      },
    })]),
    ConfigurationsActions: {
      list: jest.fn(() => Promise.resolve()),
      update: mockUpdate,
    },
  });
});

describe('SidecarConfig', () => {
  afterEach(() => jest.resetAllMocks());

  it('updates config after change', async () => {
    render(<SidecarConfig />);

    const editButton = await screen.findByRole('button', { name: /edit configuration/i });

    fireEvent.click(editButton);

    fireEvent.click(await screen.findByRole('checkbox', {
      name: /override sidecar configuration/i,
      hidden: true,
    }));

    fireEvent.click(await screen.findByRole('button', {
      name: /update configuration/i,
      hidden: true,
    }));

    await waitFor(() => { expect(mockUpdate).toHaveBeenCalledWith('org.graylog.plugins.sidecar.system.SidecarConfiguration', expect.objectContaining({ sidecar_configuration_override: true })); });
  });
});
