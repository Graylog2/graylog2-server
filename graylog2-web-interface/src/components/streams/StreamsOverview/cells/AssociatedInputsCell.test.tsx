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
import useExpandedSections from 'components/common/EntityDataTable/hooks/useExpandedSections';
import { useStreamMetricsFor } from 'components/streams/StreamsOverview/StreamMetricsContext';

import AssociatedInputsCell from './AssociatedInputsCell';

jest.mock('components/streams/StreamsOverview/StreamMetricsContext', () => ({
  useStreamMetricsFor: jest.fn(),
}));
jest.mock('components/common/EntityDataTable/hooks/useExpandedSections', () => jest.fn());

const stream = { id: 'stream-1', title: 'Test Stream' } as any;

describe('AssociatedInputsCell (Streams)', () => {
  const toggleSection = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useExpandedSections).mockReturnValue({ toggleSection, expandedSections: {} });
  });

  it('renders the count of associated inputs', () => {
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: {
        associated_inputs: [
          { id: 'input-a', type: 'input' },
          { id: 'input-b', type: 'input' },
        ],
      },
      isInitialLoading: false,
      isError: false,
    });

    render(<AssociatedInputsCell stream={stream} />);

    expect(screen.getByText('2')).toBeInTheDocument();
  });

  it('toggles the associated_inputs section on click', async () => {
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: { associated_inputs: [{ id: 'input-a', type: 'input' }] },
      isInitialLoading: false,
      isError: false,
    });

    render(<AssociatedInputsCell stream={stream} />);
    await userEvent.click(screen.getByText('1'));

    expect(toggleSection).toHaveBeenCalledWith('stream-1', 'associated_inputs');
  });

  it('renders an empty cell when there are no associated inputs', () => {
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: { associated_inputs: [] },
      isInitialLoading: false,
      isError: false,
    });

    render(<AssociatedInputsCell stream={stream} />);

    expect(screen.queryByText('0')).not.toBeInTheDocument();
    expect(screen.queryByTitle(/associated inputs/i)).not.toBeInTheDocument();
  });

  it('renders an empty cell on error', () => {
    asMock(useStreamMetricsFor).mockReturnValue({
      metrics: undefined,
      isInitialLoading: false,
      isError: true,
    });

    render(<AssociatedInputsCell stream={stream} />);

    expect(screen.queryByTitle(/associated inputs/i)).not.toBeInTheDocument();
  });
});
