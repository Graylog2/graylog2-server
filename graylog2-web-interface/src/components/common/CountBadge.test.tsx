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

import CountBadge from './CountBadge';

describe('CountBadge', () => {
  it('renders the exact count by default', () => {
    render(<CountBadge count={4134340} />);

    expect(screen.getByText('4134340')).toBeInTheDocument();
  });

  it('abbreviates large counts when opted in', () => {
    render(<CountBadge count={4134340} abbreviate />);

    expect(screen.getByText('4.1m')).toBeInTheDocument();
  });

  it('keeps counts below 1000 exact when opted in', () => {
    render(<CountBadge count={320} abbreviate />);

    expect(screen.getByText('320')).toBeInTheDocument();
  });
});
