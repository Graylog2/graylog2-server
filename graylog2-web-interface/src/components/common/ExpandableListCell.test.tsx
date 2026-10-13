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

import ExpandableListCell from './ExpandableListCell';

const items = ['Apple', 'Mango', 'Zebra'].map((name) => <span key={name}>{name}</span>);

describe('ExpandableListCell', () => {
  it('renders nothing without items', () => {
    const { container } = render(<ExpandableListCell items={[]} />);

    // The test wrapper injects theme <style> tags, so only check that no markup was rendered.
    expect(container.querySelectorAll(':not(style)')).toHaveLength(0);
  });

  it('renders a single item without toggle or count', () => {
    render(<ExpandableListCell items={items.slice(0, 1)} />);

    expect(screen.getByText('Apple')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /show all/i })).not.toBeInTheDocument();
    expect(screen.queryByText(/\+ \d/)).not.toBeInTheDocument();
  });

  it('shows only the first item and a count while collapsed', () => {
    render(<ExpandableListCell items={items} />);

    expect(screen.getByText('Apple')).toBeInTheDocument();
    expect(screen.queryByText('Mango')).not.toBeInTheDocument();
    expect(screen.getByText('+ 2')).toBeInTheDocument();
  });

  it('shows all items when expanded and collapses again', async () => {
    render(<ExpandableListCell items={items} />);

    await userEvent.click(screen.getByRole('button', { name: /show all/i }));

    expect(screen.getByText('Mango')).toBeInTheDocument();
    expect(screen.getByText('Zebra')).toBeInTheDocument();
    expect(screen.queryByText('+ 2')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /show fewer/i }));

    expect(screen.queryByText('Mango')).not.toBeInTheDocument();
    expect(screen.getByText('+ 2')).toBeInTheDocument();
  });
});
