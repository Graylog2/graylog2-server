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

import ChipsCell from 'components/common/ChipsCell';

describe('ChipsCell', () => {
  it('renders nothing when items are empty', () => {
    render(<ChipsCell items={[]} />);
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
    expect(screen.queryByText(/\+/)).not.toBeInTheDocument();
  });

  it('renders all items when count is at or below collapsedCount', () => {
    render(<ChipsCell items={['phishing', 'lateral-movement']} />);
    expect(screen.getByText('phishing')).toBeInTheDocument();
    expect(screen.getByText('lateral-movement')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Show all|Show fewer/i })).not.toBeInTheDocument();
  });

  it('truncates items above collapsedCount and shows a clickable "+ N" toggle', () => {
    const items = ['t1', 't2', 't3', 't4', 't5', 't6', 't7'];
    render(<ChipsCell items={items} />);

    expect(screen.getByText('t1')).toBeInTheDocument();
    expect(screen.getByText('t3')).toBeInTheDocument();
    expect(screen.queryByText('t4')).not.toBeInTheDocument();
    expect(screen.queryByText('t7')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Show all' })).toHaveTextContent('+ 4');
  });

  it('expands and collapses when the toggle is clicked', async () => {
    const items = ['t1', 't2', 't3', 't4', 't5', 't6', 't7'];
    render(<ChipsCell items={items} />);

    await userEvent.click(screen.getByRole('button', { name: 'Show all' }));

    expect(screen.getByText('t4')).toBeInTheDocument();
    expect(screen.getByText('t7')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Show fewer' })).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Show fewer' }));

    expect(screen.queryByText('t4')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Show all' })).toHaveTextContent('+ 4');
  });

  it('renders items as plain spans without onItemClick', () => {
    render(<ChipsCell items={['phishing']} />);
    expect(screen.queryByRole('button', { name: /Filter by/i })).not.toBeInTheDocument();
  });

  it('invokes onItemClick when a chip button is clicked', async () => {
    const onItemClick = jest.fn();
    render(<ChipsCell items={['phishing', 'exfil']} onItemClick={onItemClick} itemLabel="tag" />);

    await userEvent.click(screen.getByRole('button', { name: 'Filter by tag "phishing"' }));

    expect(onItemClick).toHaveBeenCalledWith('phishing');
  });

  it('uses a custom renderItem when supplied', () => {
    render(
      <ChipsCell items={['T1059', 'T1003']} renderItem={(t) => <span data-testid="custom-chip">CUSTOM-{t}</span>} />,
    );

    expect(screen.getByText('CUSTOM-T1003')).toBeInTheDocument();
    expect(screen.getByText('CUSTOM-T1059')).toBeInTheDocument();
    expect(screen.getAllByTestId('custom-chip')).toHaveLength(2);
  });

  it('respects a custom collapsedCount', () => {
    render(<ChipsCell items={['a', 'b', 'c', 'd']} collapsedCount={2} />);

    expect(screen.getByText('a')).toBeInTheDocument();
    expect(screen.getByText('b')).toBeInTheDocument();
    expect(screen.queryByText('c')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Show all' })).toHaveTextContent('+ 2');
  });
});
