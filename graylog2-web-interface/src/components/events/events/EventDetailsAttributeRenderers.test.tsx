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

import { getEventDetailsRenderers } from './EventDetailsAttributeRenderers';

jest.mock('components/common/EntityFilters/hooks/useUrlQueryFilters', () => () => [{ get: () => [] }, jest.fn()]);

const renderTags = (tags: Array<string>, interactiveTags?: boolean) =>
  render(<>{getEventDetailsRenderers({ interactiveTags }).tags.renderCell(tags, undefined, undefined)}</>);

describe('getEventDetailsRenderers', () => {
  it('lists all tags instead of collapsing them like the table cell', async () => {
    renderTags(['phishing', 'malware']);

    await screen.findByRole('button', { name: 'Filter by tag "phishing"' });
    await screen.findByRole('button', { name: 'Filter by tag "malware"' });

    expect(screen.queryByRole('button', { name: /show 1 more/i })).not.toBeInTheDocument();
  });

  it('renders plain, non-interactive tags when interactive tags are turned off', async () => {
    renderTags(['phishing'], false);

    await screen.findByText('phishing');

    expect(screen.queryByRole('button', { name: /Filter by/i })).not.toBeInTheDocument();
  });

  it('shows a hint when there are no tags', async () => {
    renderTags([]);

    await screen.findByText('No tags');
  });
});
