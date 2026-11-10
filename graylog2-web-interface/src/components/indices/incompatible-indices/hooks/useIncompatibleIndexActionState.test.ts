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
import { renderHook } from 'wrappedTestingLibrary/hooks';

import asMock from 'helpers/mocking/AsMock';
import useCanArchive from 'components/indices/hooks/useCanArchive';

import useIncompatibleIndexActionState from './useIncompatibleIndexActionState';
import useArchivedIndexNames from './useArchivedIndexNames';
import useCanReindex from './useCanReindex';
import usePendingIncompatibleIndexActions from './usePendingIncompatibleIndexActions';

import type { IncompatibleIndexRow } from '../fetchIncompatibleIndices';

jest.mock('components/indices/hooks/useCanArchive');
jest.mock('./useArchivedIndexNames');
jest.mock('./useCanReindex');
jest.mock('./usePendingIncompatibleIndexActions');

const makeIndex = (indexName: string): IncompatibleIndexRow => ({
  id: indexName,
  index_name: indexName,
  version: '1.3.20',
  warm_index: false,
  managed_index: true,
  system_index: false,
  active_write_index: null,
  begin: null,
  end: null,
});

const pendingReturn = {
  pendingIndexStatuses: new Map(),
  addArchiveDeleteAction: jest.fn(),
  addReindexAction: jest.fn(),
  isArchiveJobRunning: false,
  refetchClusterJobs: jest.fn(),
};

const renderState = (trackedIndices: Array<IncompatibleIndexRow> = [], isLoading = false) =>
  renderHook(() => useIncompatibleIndexActionState({ trackedIndices, isLoading }));

describe('useIncompatibleIndexActionState', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useCanArchive).mockReturnValue(true);
    asMock(useCanReindex).mockReturnValue(true);
    asMock(useArchivedIndexNames).mockReturnValue(new Set(['already_archived']));
    asMock(usePendingIncompatibleIndexActions).mockReturnValue(pendingReturn);
  });

  it('feeds the tracked indices and their names to the underlying hooks', () => {
    const index = makeIndex('graylog_0');

    renderState([index], true);

    expect(usePendingIncompatibleIndexActions).toHaveBeenCalledWith(
      expect.objectContaining({ incompatibleIndices: [index], isLoading: true, isError: false }),
    );
    expect(useArchivedIndexNames).toHaveBeenCalledWith(['graylog_0'], true);
  });

  it('is archivable only when licensed and no archive job is running', () => {
    expect(renderState().result.current.archiveActionsAvailable).toBe(true);

    asMock(usePendingIncompatibleIndexActions).mockReturnValue({ ...pendingReturn, isArchiveJobRunning: true });
    expect(renderState().result.current.archiveActionsAvailable).toBe(false);

    asMock(usePendingIncompatibleIndexActions).mockReturnValue(pendingReturn);
    asMock(useCanArchive).mockReturnValue(false);
    expect(renderState().result.current.archiveActionsAvailable).toBe(false);
  });

  it('is reindexable only when the search backend supports it', () => {
    expect(renderState().result.current.reindexActionsAvailable).toBe(true);

    asMock(useCanReindex).mockReturnValue(false);
    expect(renderState().result.current.reindexActionsAvailable).toBe(false);
  });
});
