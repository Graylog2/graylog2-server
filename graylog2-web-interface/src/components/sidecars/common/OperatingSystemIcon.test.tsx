import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';

import OperatingSystemIcon from './OperatingSystemIcon';

describe('OperatingSystemIcon', () => {
  it('returns default icon when empty operating system is passed', async () => {
    render(<OperatingSystemIcon />);
    await screen.findByText('help');
  });
});
