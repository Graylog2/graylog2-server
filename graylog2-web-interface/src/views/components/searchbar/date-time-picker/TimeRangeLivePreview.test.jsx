// @flow strict
import React from 'react';
import { render, screen } from 'wrappedTestingLibrary';

import TimeRangeLivePreview from './TimeRangeLivePreview';

type Props = {
  type: string,
  keyword?: string,
  range?: number,
  from?: string,
  to?: string,
};

describe('TimeRangeLivePreview', () => {
  it('renders relative timerange', () => {
    const timerange: Props = { type: 'relative', range: 300 };
    render(<TimeRangeLivePreview timerange={timerange} />);

    expect(screen.getByText(/5 minutes ago/i)).not.toBeNull();
  });

  it('renders absolute timerange', () => {
    const timerange: Props = { type: 'absolute', from: '1955-5-11 06:15:00.000', to: '1985-25-10 08:18:00.000' };
    render(<TimeRangeLivePreview timerange={timerange} />);

    expect(screen.getByText(/1955-5-11 06:15:00.000/i)).not.toBeNull();
    expect(screen.getByText(/1985-25-10 08:18:00.000/i)).not.toBeNull();
  });

  it('renders keyword timerange', () => {
    const timerange: Props = { type: 'keyword', keyword: 'Last ten minutes', from: '2020-10-27 15:20:56', to: '2020-10-27 15:30:56' };
    render(<TimeRangeLivePreview timerange={timerange} />);

    expect(screen.getByText(/2020-10-27 15:20:56/i)).not.toBeNull();
    expect(screen.getByText(/2020-10-27 15:30:56/i)).not.toBeNull();
  });
});
