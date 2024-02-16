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
import React from 'react';
import { render, screen } from 'wrappedTestingLibrary';

import ProgressBar from './ProgressBar';

describe('<ProgressBar />', () => {
  it('properly renders with no props', async () => {
    render(<ProgressBar />);

    const progressBar = await screen.findByRole('progressbar');

    expect(progressBar.getAttribute('value')).toBe('0');
  });

  it('properly renders with one bar', async () => {
    render(<ProgressBar bars={[{ value: 35 }]} />);

    const progressBar = await screen.findByRole('progressbar');

    expect(progressBar.getAttribute('value')).toBe('35');
  });

  it('properly renders with multiple bars', async () => {
    render(<ProgressBar bars={[{ value: 35 }, { value: 55 }]} />);

    const progressBars = await screen.findAllByRole('progressbar');

    expect((progressBars[0]).getAttribute('value')).toBe('35');
    expect((progressBars[1]).getAttribute('value')).toBe('55');
  });

  it('properly renders with label', async () => {
    render(<ProgressBar bars={[{ value: 23, label: 'Example ProgressBar' }]} />);

    const progressBar = await screen.findByRole('progressbar');

    expect(progressBar.getAttribute('value')).toBe('23');

    await screen.findByText('Example ProgressBar');
  });

  it('properly renders with animated', async () => {
    render(<ProgressBar bars={[{ value: 45, animated: true }]} />);

    const progressBar = await screen.findByRole('progressbar');

    expect(progressBar.getAttribute('value')).toBe('45');
  });

  it('properly renders with striped', async () => {
    render(<ProgressBar bars={[{ value: 56, striped: true }]} />);

    const progressBar = await screen.findByRole('progressbar');

    expect(progressBar.getAttribute('value')).toBe('56');
  });

  it('properly renders with bsStyle variant', async () => {
    render(<ProgressBar bars={[{ value: 67, bsStyle: 'danger' }]} />);

    const progressBar = await screen.findByRole('progressbar');

    expect(progressBar.getAttribute('value')).toBe('67');
  });
});
