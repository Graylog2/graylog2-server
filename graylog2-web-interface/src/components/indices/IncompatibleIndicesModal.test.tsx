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

import asMock from 'helpers/mocking/AsMock';
import useIncompatibleIndices from 'components/indices/hooks/useIncompatibleIndices';
import type { IncompatibleIndex } from 'components/indices/hooks/useIncompatibleIndices';

import IncompatibleIndicesModal from './IncompatibleIndicesModal';

jest.mock('components/indices/hooks/useIncompatibleIndices');
jest.mock('components/indices/incompatible-indices/IncompatibleIndicesTable', () => ({
  __esModule: true,
  default: jest.fn(() => <div>incompatible-indices-table</div>),
}));

const mockIncompatibleIndices = (
  indices: Array<Partial<IncompatibleIndex>>,
  { isLoading = false, isError = false } = {},
) =>
  asMock(useIncompatibleIndices).mockReturnValue({
    data: indices,
    isLoading,
    isError,
    refetch: jest.fn(),
  } as unknown as ReturnType<typeof useIncompatibleIndices>);

describe('IncompatibleIndicesModal', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('shows the incompatible indices table with local pagination state', async () => {
    mockIncompatibleIndices([{ index_name: 'graylog_0' }]);

    render(<IncompatibleIndicesModal show onClose={() => {}} />);

    expect(await screen.findByText('incompatible-indices-table')).toBeInTheDocument();
    expect(screen.getByText(/that were created with an incompatible, previous major version/i)).toBeInTheDocument();

    const { default: IncompatibleIndicesTable } = jest.requireMock(
      'components/indices/incompatible-indices/IncompatibleIndicesTable',
    );
    expect(asMock(IncompatibleIndicesTable).mock.calls[0][0]).toEqual(
      expect.objectContaining({ withoutURLParams: true }),
    );
  });

  it('reports that all indices are up to date without rendering the table', async () => {
    mockIncompatibleIndices([]);

    render(<IncompatibleIndicesModal show onClose={() => {}} />);

    expect(await screen.findByText(/all indices are up to date/i)).toBeInTheDocument();
    expect(screen.queryByText('incompatible-indices-table')).not.toBeInTheDocument();
  });

  it('shows an error without rendering the table when the indices could not be loaded', async () => {
    mockIncompatibleIndices([], { isError: true });

    render(<IncompatibleIndicesModal show onClose={() => {}} />);

    expect(await screen.findByText('Could not load incompatible indices.')).toBeInTheDocument();
    expect(screen.queryByText('incompatible-indices-table')).not.toBeInTheDocument();
  });
});
