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

import { TagsRenderer } from 'components/events/events/ColumnRenderers';

jest.mock('components/common/EntityFilters/hooks/useUrlQueryFilters', () => () => [
  { get: () => [] },
  jest.fn(),
]);

describe('TagsRenderer', () => {
  it('renders tag chips as clickable buttons by default', () => {
    render(<TagsRenderer tags={['phishing']} />);

    expect(screen.getByRole('button', { name: 'Filter by tag "phishing"' })).toBeInTheDocument();
  });

  it('renders plain, non-interactive chips when interactive is false', () => {
    render(<TagsRenderer tags={['phishing']} interactive={false} />);

    expect(screen.getByText('phishing')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Filter by/i })).not.toBeInTheDocument();
  });
});
