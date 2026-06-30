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

import LogPreviewSection from './LogPreviewSection';

describe('LogPreviewSection', () => {
  const preview = {
    messages: [
      { id: 'm1', timestamp: '2026-06-10T12:00:00.000Z', text: 'sshd[412]: Accepted publickey' },
      { id: 'm2', timestamp: '2026-06-10T11:59:00.000Z', text: 'CRON[399]: session opened' },
    ],
    total: 42,
  };

  it('renders one row per message', () => {
    render(
      <LogPreviewSection
        title="Your log sources"
        searchUrl="/search?q=x"
        preview={preview}
        isLoading={false}
        error={null}
      />,
    );

    expect(screen.getByText(/sshd\[412\]/)).toBeInTheDocument();
    expect(screen.getByText(/CRON\[399\]/)).toBeInTheDocument();
  });

  it('links to the full search page', () => {
    render(
      <LogPreviewSection
        title="Your log sources"
        searchUrl="/search?q=x"
        preview={preview}
        isLoading={false}
        error={null}
      />,
    );

    expect(screen.getByRole('link', { name: /open in search/i })).toHaveAttribute(
      'href',
      expect.stringContaining('/search?q=x'),
    );
  });

  it('shows a spinner while loading without data', () => {
    render(
      <LogPreviewSection title="Your log sources" searchUrl="/search?q=x" preview={undefined} isLoading error={null} />,
    );

    expect(screen.getByText(/loading log preview/i)).toBeInTheDocument();
  });

  it('shows the empty state when there are no messages yet', () => {
    render(
      <LogPreviewSection
        title="Your log sources"
        searchUrl="/search?q=x"
        preview={{ messages: [], total: 0 }}
        isLoading={false}
        error={null}
      />,
    );

    expect(screen.getByText(/no messages yet/i)).toBeInTheDocument();
  });

  it('shows an error only when there are no messages to show', () => {
    const { rerender } = render(
      <LogPreviewSection
        title="Your log sources"
        searchUrl="/search?q=x"
        preview={undefined}
        isLoading={false}
        error={new Error('boom')}
      />,
    );

    expect(screen.getByText(/log preview unavailable/i)).toBeInTheDocument();

    rerender(
      <LogPreviewSection
        title="Your log sources"
        searchUrl="/search?q=x"
        preview={preview}
        isLoading={false}
        error={new Error('boom')}
      />,
    );

    expect(screen.queryByText(/log preview unavailable/i)).not.toBeInTheDocument();
    expect(screen.getByText(/sshd\[412\]/)).toBeInTheDocument();
  });

  it('shows a placeholder count while the collapsible section is loading', () => {
    render(
      <LogPreviewSection
        title="Collector logs"
        searchUrl="/search?q=x"
        preview={undefined}
        isLoading
        error={null}
        collapsible
      />,
    );

    expect(screen.getByText('—')).toBeInTheDocument();
    expect(screen.queryByText('0')).not.toBeInTheDocument();
  });

  it('renders a message count in the header when collapsible', () => {
    render(
      <LogPreviewSection
        title="Collector logs"
        searchUrl="/search?q=x"
        preview={preview}
        isLoading={false}
        error={null}
        collapsible
      />,
    );

    expect(screen.getByText('42')).toBeInTheDocument();
    expect(screen.getByTestId('collapseButton')).toBeInTheDocument();
  });
});
