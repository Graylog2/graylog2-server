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
import { render, screen, waitFor, within } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import { IndexerIndices } from '@graylog/server-api';

import asMock from 'helpers/mocking/AsMock';
import type { OutdatedIndex } from 'components/indices/hooks/useOutdatedIndices';
import useOutdatedIndices from 'components/indices/hooks/useOutdatedIndices';
import useCanArchive from 'components/indices/hooks/useCanArchive';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import UserNotification from 'util/UserNotification';

import OutdatedIndicesTable from './OutdatedIndicesTable';

jest.mock('components/indices/hooks/useOutdatedIndices');
jest.mock('components/indices/hooks/useCanArchive');
jest.mock('logic/telemetry/useSendTelemetry');
jest.mock('@graylog/server-api', () => ({ IndexerIndices: { remove: jest.fn(() => Promise.resolve()) } }));
jest.mock('util/UserNotification', () => ({ success: jest.fn(), error: jest.fn() }));

const makeIndex = (overrides: Partial<OutdatedIndex>): OutdatedIndex => ({
  index_name: 'index',
  version: '7.10.2',
  warm_index: false,
  managed_index: false,
  system_index: false,
  ...overrides,
});

const graylogIndex = makeIndex({ index_name: 'graylog_0', managed_index: true });
const systemIndex = makeIndex({ index_name: '.system-index', system_index: true });
const foreignIndex = makeIndex({ index_name: 'legacy-index' });

const mockOutdatedIndices = (overrides: Partial<ReturnType<typeof useOutdatedIndices>>) => {
  asMock(useOutdatedIndices).mockReturnValue({
    data: [],
    isError: false,
    isLoading: false,
    refetch: jest.fn(() => Promise.resolve({ data: [] })),
    ...overrides,
  } as ReturnType<typeof useOutdatedIndices>);
};

describe('OutdatedIndicesTable', () => {
  beforeEach(() => {
    asMock(useCanArchive).mockReturnValue(true);
    asMock(useSendTelemetry).mockReturnValue(jest.fn());
    mockOutdatedIndices({});
  });

  it('shows a spinner while loading', async () => {
    mockOutdatedIndices({ isLoading: true });
    render(<OutdatedIndicesTable />);

    expect(await screen.findByText(/loading outdated indices/i)).toBeInTheDocument();
  });

  it('shows an error alert when loading fails', () => {
    mockOutdatedIndices({ isError: true });
    render(<OutdatedIndicesTable />);

    expect(screen.getByText(/could not load outdated indices/i)).toBeInTheDocument();
  });

  it('shows a success message when there are no outdated indices', () => {
    mockOutdatedIndices({ data: [] });
    render(<OutdatedIndicesTable />);

    expect(screen.getByText(/no outdated indices found/i)).toBeInTheDocument();
  });

  it('renders the group counts and the default group rows', () => {
    mockOutdatedIndices({ data: [graylogIndex, systemIndex] });
    render(<OutdatedIndicesTable />);

    expect(screen.getByText('Graylog (1)')).toBeInTheDocument();
    expect(screen.getByText('System (1)')).toBeInTheDocument();
    expect(screen.getByText('Foreign (0)')).toBeInTheDocument();
    expect(screen.getByText('graylog_0')).toBeInTheDocument();
  });

  it('offers archive-and-delete for managed indices only when archiving is available', () => {
    mockOutdatedIndices({ data: [graylogIndex] });
    const { rerender } = render(<OutdatedIndicesTable />);

    expect(screen.getByRole('button', { name: /archive and delete/i })).toBeInTheDocument();

    asMock(useCanArchive).mockReturnValue(false);
    rerender(<OutdatedIndicesTable />);

    expect(screen.queryByRole('button', { name: /archive and delete/i })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^delete$/i })).toBeInTheDocument();
  });

  it('only offers reindex for system indices', () => {
    mockOutdatedIndices({ data: [systemIndex] });
    render(<OutdatedIndicesTable />);

    expect(screen.getByRole('button', { name: /reindex/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /delete/i })).not.toBeInTheDocument();
  });

  it('only offers delete for foreign indices', () => {
    mockOutdatedIndices({ data: [foreignIndex] });
    render(<OutdatedIndicesTable />);

    expect(screen.getByRole('button', { name: /^delete$/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /archive/i })).not.toBeInTheDocument();
  });

  it('runs the delete action, sends telemetry and notifies on confirm', async () => {
    const sendTelemetry = jest.fn();
    asMock(useSendTelemetry).mockReturnValue(sendTelemetry);
    mockOutdatedIndices({ data: [foreignIndex] });
    render(<OutdatedIndicesTable />);

    await userEvent.click(screen.getByRole('button', { name: /^delete$/i }));

    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText(/this will permanently delete/i)).toBeInTheDocument();

    await userEvent.click(within(dialog).getByRole('button', { name: /^delete$/i }));

    await waitFor(() => expect(IndexerIndices.remove).toHaveBeenCalledWith('legacy-index'));
    expect(sendTelemetry).toHaveBeenCalledWith(
      TELEMETRY_EVENT_TYPE.DATANODE_OPENSEARCH_UPGRADE.INDEX_DELETE_CONFIRMED,
      expect.objectContaining({ app_section: 'opensearch-upgrade' }),
    );
    expect(UserNotification.success).toHaveBeenCalled();
  });
});
