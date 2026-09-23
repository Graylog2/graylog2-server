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

import EventTypeLabel, { getEventTypeName } from './EventTypeLabel';

describe('getEventTypeName', () => {
  it('resolves boolean and string true/false to their name', () => {
    expect(getEventTypeName(true)).toBe('Alert');
    expect(getEventTypeName('true')).toBe('Alert');
    expect(getEventTypeName(false)).toBe('Event');
    expect(getEventTypeName('false')).toBe('Event');
  });

  it('passes an unrecognized value through unchanged', () => {
    expect(getEventTypeName('count()')).toBe('count()');
  });
});

describe('EventTypeLabel', () => {
  it('shows "Alert" for a true value', async () => {
    render(<EventTypeLabel isAlert />);
    await screen.findByText('Alert');
  });

  it('shows "Event" for a false value', async () => {
    render(<EventTypeLabel isAlert={false} />);
    await screen.findByText('Event');
  });
});
