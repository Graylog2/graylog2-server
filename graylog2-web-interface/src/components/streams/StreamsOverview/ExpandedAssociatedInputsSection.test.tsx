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
import useEntityTitles from 'hooks/useEntityTitles';
import { useStreamMetricsFor } from 'components/streams/StreamsOverview/StreamMetricsContext';

import ExpandedAssociatedInputsSection from './ExpandedAssociatedInputsSection';

jest.mock('components/streams/StreamsOverview/StreamMetricsContext', () => ({
  useStreamMetricsFor: jest.fn(),
}));
jest.mock('hooks/useEntityTitles', () => jest.fn());

const stream = { id: 'stream-1', title: 'Test Stream' } as any;

describe('ExpandedAssociatedInputsSection', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useEntityTitles).mockReturnValue({
      titlesById: { 'input-a': 'Input Alpha' },
      notPermittedIds: new Set(),
      isInitialLoading: false,
      isError: false,
    });
  });

  it('renders linked input titles for each associated input', () => {
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: { associated_inputs: ['input-a'] },
      isInitialLoading: false,
      isError: false,
    });

    render(<ExpandedAssociatedInputsSection stream={stream} />);

    expect(screen.getByRole('link', { name: 'Input Alpha' })).toBeInTheDocument();
  });

  it('renders "<id> (deleted)" for inputs whose title is not resolved', () => {
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: { associated_inputs: ['input-deleted'] },
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
