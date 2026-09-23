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
import useRunsWithDataNode from 'components/datanode/hooks/useRunsWithDataNode';
import useIncompatibleIndices from 'components/indices/hooks/useIncompatibleIndices';
import type { IncompatibleIndex } from 'components/indices/hooks/useIncompatibleIndices';
import useSearchVersionCheck from 'hooks/useSearchVersionCheck';

import IncompatibleIndicesModal from './IncompatibleIndicesModal';

jest.mock('components/indices/incompatible-indices/IncompatibleIndicesTable', () => ({
  __esModule: true,
  default: jest.fn(() => <div>incompatible-indices-table</div>),
}));
jest.mock('components/datanode/hooks/useRunsWithDataNode');
jest.mock('components/indices/hooks/useIncompatibleIndices');
jest.mock('hooks/useSearchVersionCheck');

const incompatibleIndex: IncompatibleIndex = {
  index_name: 'graylog_0',
  version: '2.19.0',
  warm_index: false,
  managed_index: true,
  system_index: false,
  active_write_index: null,
};

const mockIncompatibleIndices = (indices: Array<IncompatibleIndex> = []) =>
  asMock(useIncompatibleIndices).mockReturnValue({
    data: indices,
    isError: false,
    isLoading: false,
    refetch: jest.fn(),
  });

const mockSearchVersionCheck = (satisfied: boolean | undefined) =>
  asMock(useSearchVersionCheck).mockReturnValue({
    data: satisfied === undefined ? undefined : { satisfied },
    isLoading: satisfied === undefined,
    error: null,
  });

describe('IncompatibleIndicesModal', () => {
  beforeEach(() => {
    asMock(useRunsWithDataNode).mockReturnValue({ data: false, isLoading: false });
    mockIncompatibleIndices();
    mockSearchVersionCheck(true);
  });

  it('shows the incompatible indices table with local pagination state', async () => {
    render(<IncompatibleIndicesModal show onClose={() => {}} />);

    expect(await screen.findByText('incompatible-indices-table')).toBeInTheDocument();
    expect(screen.getByText(/need to be archived, deleted or reindexed/i)).toBeInTheDocument();

    const { default: IncompatibleIndicesTable } = jest.requireMock(
      'components/indices/incompatible-indices/IncompatibleIndicesTable',
    );
    expect(asMock(IncompatibleIndicesTable).mock.calls[0][0]).toEqual(
      expect.objectContaining({ withoutURLParams: true }),
    );
  });

  it('warns against upgrading an OpenSearch cluster with incompatible indices', () => {
    mockIncompatibleIndices([incompatibleIndex]);

    render(<IncompatibleIndicesModal show onClose={() => {}} />);

    const warningCopy = screen.getByText(/prevents this cluster from upgrading to OpenSearch 3.x/i);

    expect(warningCopy).toBeInTheDocument();
    expect(screen.getByText('migrate from OpenSearch to Data Node')).toBeInTheDocument();
    const documentationLink = screen.getByRole('link', { name: /documentation.*opens in a new tab/i });

    expect(documentationLink).toHaveAttribute('href', expect.stringContaining('data_node_in-place_migration.htm'));
    expect(documentationLink).toHaveAttribute('target', '_blank');
    expect(documentationLink).toHaveAttribute('rel', 'noopener noreferrer');
    expect(screen.getByText(/OpenSearch 2.x remains in support until November 2027/i)).toBeInTheDocument();
  });

  it('does not show the warning when there are no incompatible indices', () => {
    render(<IncompatibleIndicesModal show onClose={() => {}} />);

    expect(screen.queryByText(/prevents this cluster from upgrading to OpenSearch 3.x/i)).not.toBeInTheDocument();
  });

  it('does not show the warning for a non-OpenSearch search backend', () => {
    mockIncompatibleIndices([incompatibleIndex]);
    mockSearchVersionCheck(false);

    render(<IncompatibleIndicesModal show onClose={() => {}} />);

    expect(screen.queryByText(/prevents this cluster from upgrading to OpenSearch 3.x/i)).not.toBeInTheDocument();
  });

  it('does not show the warning when using Data Node', () => {
    asMock(useRunsWithDataNode).mockReturnValue({ data: true, isLoading: false });
    mockIncompatibleIndices([incompatibleIndex]);

    render(<IncompatibleIndicesModal show onClose={() => {}} />);

    expect(screen.queryByText(/prevents this cluster from upgrading to OpenSearch 3.x/i)).not.toBeInTheDocument();
  });
});
