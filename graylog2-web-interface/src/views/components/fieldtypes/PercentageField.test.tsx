import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';

import PercentageField from './PercentageField';

describe('PercentageField', () => {
  it('does not show very small values as `NaN%`', async () => {
    render(<PercentageField value={2.744906525058769E-9} />);
    await screen.findByText('0.00%');
  });
});
