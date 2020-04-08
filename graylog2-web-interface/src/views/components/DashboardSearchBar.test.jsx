// @flow strict
import * as React from 'react';
import { render } from '@testing-library/react';

import DashboardSearchBar from './DashboardSearchBar';

describe('DashboardSearchBar', () => {
  it('should do something', () => {
    const { container } = render(<DashboardSearchBar/>);
    expect(container).not.toBeNull();
  });
});
