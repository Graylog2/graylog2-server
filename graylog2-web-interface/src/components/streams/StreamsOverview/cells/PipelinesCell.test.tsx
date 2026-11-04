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

import { asMock } from 'helpers/mocking';
import useEntityTitles from 'hooks/useEntityTitles';
import { useStreamMetricsFor } from 'components/streams/StreamsOverview/StreamMetricsContext';

import PipelinesCell from './PipelinesCell';

jest.mock('components/streams/StreamsOverview/StreamMetricsContext', () => ({
  useStreamMetricsFor: jest.fn(),
}));
jest.mock('hooks/useEntityTitles');

const stream = { id: 'stream-1', title: 'Test Stream', is_default: false, is_editable: true } as any;

const mockPipelines = (pipelineIds: Array<string>) =>
  asMock(useStreamMetricsFor).mockReturnValue({
    metrics: { pipelines: pipelineIds },
    isInitialLoading: false,
    isError: false,
  });

const mockTitles = (titlesById: Record<string, string>, notPermittedIds: Array<string> = []) =>
  asMock(useEntityTitles).mockReturnValue({
    titlesById,
    notPermittedIds: new Set(notPermittedIds),
    isInitialLoading: false,
    isFetching: false,
    isError: false,
  });

describe('PipelinesCell (Streams)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockTitles({ 'p-1': 'Alpha Pipeline', 'p-2': 'Beta Pipeline' });
  });

  it('renders the pipelines of the default stream (#14572)', async () => {
    mockPipelines(['p-1']);

    render(<PipelinesCell stream={{ ...stream, is_default: true } as any} />);

    await screen.findByRole('link', { name: 'Alpha Pipeline' });
  });

  it('renders nothing when the stream is not editable', () => {
    mockPipelines(['p-1']);

    render(<PipelinesCell stream={{ ...stream, is_editable: false } as any} />);

    expect(screen.queryByText('Alpha Pipeline')).not.toBeInTheDocument();
  });

  it('renders an empty cell when there are zero connected pipelines', () => {
    mockPipelines([]);

    render(<PipelinesCell stream={stream} />);

    expect(screen.queryByRole('link')).not.toBeInTheDocument();
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('renders the first pipeline and the number of the others', async () => {
    mockPipelines(['p-2', 'p-1']);

    render(<PipelinesCell stream={stream} />);

    await screen.findByRole('link', { name: 'Alpha Pipeline' });

    expect(await screen.findByRole('button', { name: /show 1 more/i })).toHaveTextContent('+1');
    expect(screen.queryByText('Beta Pipeline')).not.toBeInTheDocument();
  });

  it('links each pipeline to its detail page when the list is expanded', async () => {
    mockPipelines(['p-1', 'p-2']);

    render(<PipelinesCell stream={stream} />);

    await userEvent.click(await screen.findByRole('button', { name: /show 1 more/i }));

    expect(await screen.findByRole('link', { name: 'Alpha Pipeline' })).toHaveAttribute(
      'href',
      '/system/pipelines/p-1',
    );
    expect(await screen.findByRole('link', { name: 'Beta Pipeline' })).toHaveAttribute('href', '/system/pipelines/p-2');
  });

  it('lists deleted pipelines and pipelines the user may not read without a link', async () => {
    mockTitles({}, ['p-secret']);
    mockPipelines(['p-deleted', 'p-secret']);

    render(<PipelinesCell stream={stream} />);

    await userEvent.click(await screen.findByRole('button', { name: /show 1 more/i }));

    await screen.findByText('p-deleted (deleted)');

    expect(await screen.findByTitle('Missing permissions')).toHaveTextContent('p-secret');
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });
});
