// @flow strict
import * as React from 'react';
import { render } from '@testing-library/react';

import ReplaySearchButton from './ReplaySearchButton';

describe('ReplaySearchButton', () => {
  it('should do something', () => {
    const { container } = render(<ReplaySearchButton/>);
    expect(container).not.toBeNull();
  });
});
