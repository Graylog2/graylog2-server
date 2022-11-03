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

import SidecarConfig from './SidecarConfig';

describe('SidecarConfig', () => {
  it('updates config after change', async () => {
    const updateConfig = jest.fn(() => Promise.resolve());
    render(<SidecarConfig updateConfig={updateConfig} />);

    const openButton = await screen.findByRole('button', { name: /edit configuration/i });
    fireEvent.click(openButton);

    fireEvent.click(await screen.findByRole('checkbox', {
      name: /override sidecar configuration/i,
      hidden: true,
    }));

    fireEvent.click(await screen.findByRole('button', {
      name: /update configuration/i,
      hidden: true,
    }));

    await waitFor(() => { expect(updateConfig).toHaveBeenCalledWith(expect.objectContaining({ sidecar_configuration_override: true })); });
  });
});
