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
