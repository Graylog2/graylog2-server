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
import { render, screen } from 'wrappedTestingLibrary';

import PipelineLoadCell from './PipelineLoadCell';

describe('PipelineLoadCell', () => {
  it('formats the load percent to two decimals', () => {
    render(<PipelineLoadCell loadPercent={12.3456} />);

    expect(screen.getByText('12.35%')).toBeInTheDocument();
  });

  it('renders 0.00% for zero-cost participating rows', () => {
    render(<PipelineLoadCell loadPercent={0} />);

    expect(screen.getByText('0.00%')).toBeInTheDocument();
  });

  it('renders nothing when load percent is undefined', () => {
    render(<PipelineLoadCell loadPercent={undefined} />);

    expect(screen.queryByText(/%$/)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/Pipeline Load is unavailable/i)).not.toBeInTheDocument();
  });

  it('renders an em-dash with an accessible error label when error is true', () => {
    render(<PipelineLoadCell loadPercent={undefined} error />);

    const errorEl = screen.getByLabelText(/Pipeline Load is unavailable/i);
    expect(errorEl).toBeInTheDocument();
    expect(errorEl).toHaveTextContent('—');
  });

  it('prefers the error state over a numeric value', () => {
    render(<PipelineLoadCell loadPercent={50} error />);

    expect(screen.queryByText('50.00%')).not.toBeInTheDocument();
    expect(screen.getByLabelText(/Pipeline Load is unavailable/i)).toBeInTheDocument();
  });
});
