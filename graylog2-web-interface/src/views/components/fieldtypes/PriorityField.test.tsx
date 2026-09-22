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

import PriorityField from './PriorityField';

describe('PriorityField', () => {
  it('shows the priority name for a known value', async () => {
    render(<PriorityField value={4} />);
    await screen.findByText('Critical');
  });

  it('falls back to the raw value for an unknown priority', async () => {
    render(<PriorityField value={99} />);
    await screen.findByText('99');
  });
});
