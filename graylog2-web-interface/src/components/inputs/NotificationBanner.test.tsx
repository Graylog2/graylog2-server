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

import NotificationBanner from './NotificationBanner';
import type { NotificationItem } from './NotificationBanner';

describe('<NotificationBanner>', () => {
  it('renders nothing when items is empty', () => {
    render(<NotificationBanner title="Test title" items={[]} />);

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('renders the title text', () => {
    const items: Array<NotificationItem> = [
      { severity: 'danger', message: 'something failed' },
    ];

    render(<NotificationBanner title="One or more inputs are currently" items={items} />);

    expect(screen.getByText('One or more inputs are currently')).toBeInTheDocument();
  });

  it('renders a single danger notification', () => {
    const items: Array<NotificationItem> = [
      { severity: 'danger', message: 'in failed state. Failed inputs will not receive traffic.' },
    ];

    render(<NotificationBanner title="Test" items={items} />);

    expect(screen.getByText('in failed state. Failed inputs will not receive traffic.')).toBeInTheDocument();
    expect(screen.getAllByRole('alert')).toHaveLength(1);
  });

  it('renders a single warning notification', () => {
    const items: Array<NotificationItem> = [
      { severity: 'warning', message: 'stopped. Stopped inputs will not receive traffic.' },
    ];

    render(<NotificationBanner title="Test" items={items} />);

    expect(screen.getByText('stopped. Stopped inputs will not receive traffic.')).toBeInTheDocument();
    expect(screen.getAllByRole('alert')).toHaveLength(1);
  });

  it('renders multiple notifications in a single alert', () => {
    const items: Array<NotificationItem> = [
      { severity: 'danger', message: 'in failed state.' },
      { severity: 'warning', message: 'initializing.' },
      { severity: 'warning', message: 'stopped.' },
    ];

    render(<NotificationBanner title="Test" items={items} />);

    expect(screen.getByText('in failed state.')).toBeInTheDocument();
    expect(screen.getByText('initializing.')).toBeInTheDocument();
    expect(screen.getByText('stopped.')).toBeInTheDocument();
    expect(screen.getAllByRole('alert')).toHaveLength(1);
  });
});
