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

import FirstUseWelcome from './FirstUseWelcome';

describe('FirstUseWelcome', () => {
  it('links the "Set up Collector" button to the collectors overview', async () => {
    render(<FirstUseWelcome />);

    const link = await screen.findByRole('link', { name: /Set up Collector/i });

    expect(link).toHaveAttribute('href', '/system/collectors');
  });

  it('shows the supported platform icons', () => {
    render(<FirstUseWelcome />);

    expect(screen.getByTitle('Linux')).toBeInTheDocument();
    expect(screen.getByTitle('Windows')).toBeInTheDocument();
  });
});
