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

import TagList from 'components/common/TagList';

describe('TagList', () => {
  it('renders the empty fallback when provided and tags are empty', () => {
    render(<TagList tags={[]} emptyFallback={<em>No tags</em>} />);
    expect(screen.getByText('No tags')).toBeInTheDocument();
  });

  it('renders nothing visible when tags are empty and no fallback is given', () => {
    render(<TagList tags={[]} />);
    expect(screen.queryByText('No tags')).not.toBeInTheDocument();
  });

  it('renders nothing visible when tags are nullish and no fallback is given', () => {
    render(<TagList tags={null} />);
    expect(screen.queryByText('No tags')).not.toBeInTheDocument();
  });

  it('renders one chip per tag', () => {
    render(<TagList tags={['phishing', 'lateral-movement']} />);
    expect(screen.getByText('phishing')).toBeInTheDocument();
    expect(screen.getByText('lateral-movement')).toBeInTheDocument();
  });
});
