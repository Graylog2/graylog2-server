// @flow strict
import * as React from 'react';
import { render } from '@testing-library/react';

import ReplaySearchButton from './ReplaySearchButton';

describe('ReplaySearchButton', () => {
  it('renders play button', () => {
    const { getByTitle } = render(<ReplaySearchButton />);
    expect(getByTitle("Replay search")).not.toBeNull();
  });
});
