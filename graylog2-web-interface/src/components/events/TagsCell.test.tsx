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

import TagsCell from 'components/events/TagsCell';

describe('TagsCell', () => {
  it('renders nothing when tags are empty', () => {
    render(<TagsCell tags={[]} />);
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
    expect(screen.queryByText(/\+/)).not.toBeInTheDocument();
  });

  it('renders all tags when count is at or below collapsedCount', () => {
    render(<TagsCell tags={['phishing', 'lateral-movement']} />);
    expect(screen.getByText('phishing')).toBeInTheDocument();
    expect(screen.getByText('lateral-movement')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Show all tags|Show fewer tags/i })).not.toBeInTheDocument();
  });

  it('truncates tags above collapsedCount and shows a clickable "+ N" toggle', () => {
    const tags = ['t1', 't2', 't3', 't4', 't5', 't6', 't7'];
    render(<TagsCell tags={tags} />);

    expect(screen.getByText('t1')).toBeInTheDocument();
    expect(screen.getByText('t3')).toBeInTheDocument();
    expect(screen.queryByText('t4')).not.toBeInTheDocument();
    expect(screen.queryByText('t7')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Show all tags' })).toHaveTextContent('+ 4');
  });

  it('expands and collapses when the toggle is clicked', async () => {
    const tags = ['t1', 't2', 't3', 't4', 't5', 't6', 't7'];
    render(<TagsCell tags={tags} />);

    await userEvent.click(screen.getByRole('button', { name: 'Show all tags' }));

    expect(screen.getByText('t4')).toBeInTheDocument();
    expect(screen.getByText('t7')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Show fewer tags' })).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Show fewer tags' }));

    expect(screen.queryByText('t4')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Show all tags' })).toHaveTextContent('+ 4');
  });

  it('renders tags as plain spans without onTagClick', () => {
    render(<TagsCell tags={['phishing']} />);
    expect(screen.queryByRole('button', { name: /Filter by tag/i })).not.toBeInTheDocument();
  });

  it('invokes onTagClick when a tag button is clicked', async () => {
    const onTagClick = jest.fn();
    render(<TagsCell tags={['phishing', 'exfil']} onTagClick={onTagClick} />);

    await userEvent.click(screen.getByRole('button', { name: 'Filter by tag "phishing"' }));

    expect(onTagClick).toHaveBeenCalledWith('phishing');
  });

  it('respects a custom collapsedCount', () => {
    render(<TagsCell tags={['a', 'b', 'c', 'd']} collapsedCount={2} />);

    expect(screen.getByText('a')).toBeInTheDocument();
    expect(screen.getByText('b')).toBeInTheDocument();
    expect(screen.queryByText('c')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Show all tags' })).toHaveTextContent('+ 2');
  });
});
