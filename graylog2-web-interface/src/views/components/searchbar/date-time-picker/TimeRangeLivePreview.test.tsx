// @flow strict
import React from 'react';
import { render, screen } from 'wrappedTestingLibrary';

import type { RelativeTimeRange, AbsoluteTimeRange, KeywordTimeRange } from 'views/logic/queries/Query';

import TimeRangeLivePreview from './TimeRangeLivePreview';

describe('TimeRangeLivePreview', () => {
  it('renders relative timerange', () => {
    const timerange: RelativeTimeRange = { type: 'relative', range: 300 };
    render(<TimeRangeLivePreview timerange={timerange} />);

    expect(screen.getByText(/5 minutes ago/i)).not.toBeNull();
  });

  it('renders absolute timerange', () => {
    const timerange: AbsoluteTimeRange = {
      type: 'absolute',
      from: '1955-11-05 06:15:00.000',
      to: '1985-10-25 08:18:00.000',
    };
    render(<TimeRangeLivePreview timerange={timerange} />);

    expect(screen.getByText(/1955-11-05 06:15:00.000/i)).not.toBeNull();
    expect(screen.getByText(/1985-10-25 08:18:00.000/i)).not.toBeNull();
  });

  it('renders keyword timerange', () => {
    const timerange: KeywordTimeRange = { type: 'keyword', keyword: 'Last ten minutes', from: '2020-10-27 15:20:56', to: '2020-10-27 15:30:56' };
    render(<TimeRangeLivePreview timerange={timerange} />);

    expect(screen.getByText(/2020-10-27 15:20:56/i)).not.toBeNull();
    expect(screen.getByText(/2020-10-27 15:30:56/i)).not.toBeNull();
  });
});
