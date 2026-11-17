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
import userEvent from '@testing-library/user-event';

import ViewerBadges from './ViewerBadges';

const currentUser = { id: 'admin-id', username: 'admin', full_name: 'Administrator' };
const viewer = (index: number) => ({ id: `user-${index}`, username: `user${index}`, full_name: `Jane Doe${index}` });

describe('ViewerBadges', () => {
  it('renders nothing without viewers', () => {
    render(<ViewerBadges viewers={[]} />);

    expect(screen.queryByRole('group', { name: 'Current viewers' })).not.toBeInTheDocument();
  });

  it('renders initials of viewers with the current user first', () => {
    render(<ViewerBadges viewers={[viewer(1), currentUser]} />);

    const badges = screen.getAllByRole('img');

    expect(badges.map((badge) => badge.getAttribute('aria-label'))).toEqual([
      'You (Administrator)',
      'Jane Doe1 (user1)',
    ]);
    expect(screen.getByText('AD')).toBeInTheDocument();
    expect(screen.getByText('JD')).toBeInTheDocument();
  });

  it('shows the full name on hover', async () => {
    render(<ViewerBadges viewers={[viewer(1)]} />);

    await userEvent.hover(screen.getByText('JD'));

    await screen.findByRole('tooltip', { name: 'Jane Doe1 (user1)' });
  });

  it('collapses viewers beyond the maximum and keeps the current user visible', async () => {
    render(<ViewerBadges viewers={[viewer(1), viewer(2), viewer(3), currentUser]} max={3} />);

    expect(screen.getByRole('img', { name: 'You (Administrator)' })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Jane Doe1 (user1)' })).toBeInTheDocument();
    expect(screen.queryByRole('img', { name: 'Jane Doe2 (user2)' })).not.toBeInTheDocument();

    await userEvent.hover(screen.getByText('+2'));

    const tooltip = await screen.findByRole('tooltip');

    expect(tooltip).toHaveTextContent('Jane Doe2 (user2)');
    expect(tooltip).toHaveTextContent('Jane Doe3 (user3)');
  });
});
