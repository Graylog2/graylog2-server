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

import { asMock } from 'helpers/mocking';
import useInputDetails from 'hooks/useInputDetails';
import { useStreamMetricsFor } from 'components/streams/StreamsOverview/StreamMetricsContext';
import { useInputTitleLinkBuilder } from 'components/streams/StreamsOverview/inputTitleLinks';

import ExpandedAssociatedInputsSection from './ExpandedAssociatedInputsSection';

// Test-local augmentation so this open-source test can exercise the forwarder code path that
// enterprise contributes at runtime via the same module augmentation.
declare module 'hooks/useInputDetails' {
  interface ResolvedInputMap {
    forwarder_input: { id: string; title: string; profile_id: string | null };
  }
}

jest.mock('components/streams/StreamsOverview/StreamMetricsContext', () => ({
  useStreamMetricsFor: jest.fn(),
}));
jest.mock('hooks/useInputDetails', () => jest.fn());
jest.mock('components/streams/StreamsOverview/inputTitleLinks', () => ({
  useInputTitleLinkBuilder: jest.fn(),
}));

const stream = { id: 'stream-1', title: 'Test Stream' } as any;

describe('ExpandedAssociatedInputsSection', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useInputDetails).mockReturnValue({
      resolvedById: {
        'input-a': { type: 'input', id: 'input-a', title: 'Input Alpha' },
        'forwarder-a': {
          type: 'forwarder_input',
          id: 'forwarder-a',
          title: 'Forwarder Alpha',
          profile_id: 'profile-1',
        },
      },
      isInitialLoading: false,
      isFetching: false,
      isError: false,
    });
    asMock(useInputTitleLinkBuilder).mockReturnValue((resolved) => {
      if (resolved.type === 'input') return `/system/input/diagnosis/${resolved.id}`;

      if (resolved.type === 'forwarder_input' && resolved.profile_id) {
        return `/system/input_profiles/${resolved.profile_id}`;
      }

      return null;
    });
  });

  it('renders linked regular input title', () => {
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: { associated_inputs: [{ id: 'input-a', type: 'input' }] },
      isInitialLoading: false,
      isError: false,
    });

    render(<ExpandedAssociatedInputsSection stream={stream} />);

    const link = screen.getByRole('link', { name: 'Input Alpha' });
    expect(link).toBeInTheDocument();
    expect(link).toHaveAttribute('href', '/system/input/diagnosis/input-a');
  });

  it('links a forwarder input to its input profile page', () => {
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: { associated_inputs: [{ id: 'forwarder-a', type: 'forwarder_input' }] },
      isInitialLoading: false,
      isError: false,
    });

    render(<ExpandedAssociatedInputsSection stream={stream} />);

    const link = screen.getByRole('link', { name: 'Forwarder Alpha' });
    expect(link).toHaveAttribute('href', '/system/input_profiles/profile-1');
  });

  it('renders forwarder input title without a link when the profile is not readable', () => {
    asMock(useInputDetails).mockReturnValue({
      resolvedById: {
        'forwarder-unlinkable': {
          type: 'forwarder_input',
          id: 'forwarder-unlinkable',
          title: 'Forwarder Without Profile Access',
          profile_id: null,
        },
      },
      isInitialLoading: false,
      isFetching: false,
      isError: false,
    });
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: { associated_inputs: [{ id: 'forwarder-unlinkable', type: 'forwarder_input' }] },
      isInitialLoading: false,
      isError: false,
    });

    render(<ExpandedAssociatedInputsSection stream={stream} />);

    expect(screen.getByText('Forwarder Without Profile Access')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Forwarder Without Profile Access' })).not.toBeInTheDocument();
  });

  it('shows the bare id (not "deleted") while details are still being fetched', () => {
    // isFetching covers both initial loads and background refetches — guards against
    // transiently labelling a freshly-appearing ID as deleted before its details settle.
    asMock(useInputDetails).mockReturnValue({
      resolvedById: {},
      isInitialLoading: false,
      isFetching: true,
      isError: false,
    });
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: { associated_inputs: [{ id: 'input-x', type: 'input' }] },
      isInitialLoading: false,
      isError: false,
    });

    render(<ExpandedAssociatedInputsSection stream={stream} />);

    expect(screen.getByText('input-x')).toBeInTheDocument();
    expect(screen.queryByText(/deleted/i)).not.toBeInTheDocument();
  });

  it('shows the bare id (not "deleted") when the details request errored', () => {
    asMock(useInputDetails).mockReturnValue({
      resolvedById: {},
      isInitialLoading: false,
      isFetching: false,
      isError: true,
    });
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: { associated_inputs: [{ id: 'input-x', type: 'input' }] },
      isInitialLoading: false,
      isError: false,
    });

    render(<ExpandedAssociatedInputsSection stream={stream} />);

    expect(screen.getByText('input-x')).toBeInTheDocument();
    expect(screen.queryByText(/deleted/i)).not.toBeInTheDocument();
  });

  it('shows "<id> (deleted)" once details have settled with no entry for the ID', () => {
    // The cached associated_inputs entry can outlive the underlying input (cache TTL is hours).
    // Once the details fetch settles without returning the ID, the input is genuinely gone.
    asMock(useInputDetails).mockReturnValue({
      resolvedById: {},
      isInitialLoading: false,
      isFetching: false,
      isError: false,
    });
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: { associated_inputs: [{ id: 'input-deleted', type: 'input' }] },
      isInitialLoading: false,
      isError: false,
    });

    render(<ExpandedAssociatedInputsSection stream={stream} />);

    expect(screen.getByText('input-deleted (deleted)')).toBeInTheDocument();
  });

  it('renders empty-state when there are no associated inputs', () => {
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: { associated_inputs: [] },
      isInitialLoading: false,
      isError: false,
    });

    render(<ExpandedAssociatedInputsSection stream={stream} />);

    expect(screen.getByText(/no inputs have sent messages/i)).toBeInTheDocument();
  });
});
