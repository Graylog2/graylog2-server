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
import React, { useCallback, useMemo, useState } from 'react';
import styled, { css } from 'styled-components';
import { useQueryClient } from '@tanstack/react-query';

import { PaginatedEntityTable } from 'components/common';
import useCanArchive from 'components/indices/hooks/useCanArchive';

import {
  fetchIncompatibleIndices,
  incompatibleIndicesKeyFn,
  INCOMPATIBLE_INDICES_QUERY_KEY,
} from './fetchIncompatibleIndices';
import type { IncompatibleIndexRow, IncompatibleIndicesResponse } from './fetchIncompatibleIndices';
import { createColumnRenderers, DEFAULT_DISPLAYED_COLUMNS } from './IncompatibleIndicesColumnRenderers';
import IncompatibleIndexTableActions from './IncompatibleIndexTableActions';
import IncompatibleIndicesBulkActions from './IncompatibleIndicesBulkActions';
import useArchivedIndexNames from './hooks/useArchivedIndexNames';
import usePendingIncompatibleIndexActions from './hooks/usePendingIncompatibleIndexActions';

const Heading = styled.h4(
  ({ theme }) => css`
    margin-top: ${theme.spacings.md};
    margin-bottom: ${theme.spacings.sm};
  `,
);

const TABLE_LAYOUT = {
  entityTableId: 'incompatible_indices',
  defaultSort: { attributeId: 'index_name', direction: 'asc' as const },
  defaultDisplayedAttributes: DEFAULT_DISPLAYED_COLUMNS,
  defaultPageSize: 20,
  defaultColumnOrder: ['index_name', 'category', 'version', 'begin', 'end'],
};

const IncompatibleIndicesTable = () => {
  const queryClient = useQueryClient();
  const canArchive = useCanArchive();
  const [loadedIndices, setLoadedIndices] = useState<Array<IncompatibleIndexRow>>([]);
  const [hasLoaded, setHasLoaded] = useState(false);

  const refetch = useCallback(
    () => queryClient.invalidateQueries({ queryKey: INCOMPATIBLE_INDICES_QUERY_KEY }),
    [queryClient],
  );

  const incompatibleIndexNames = useMemo(() => loadedIndices.map((index) => index.index_name), [loadedIndices]);
  const archivedIndexNames = useArchivedIndexNames(incompatibleIndexNames, canArchive);
  const { pendingIndexStatuses, addArchiveDeleteAction, isArchiveJobRunning, refetchClusterJobs } =
    usePendingIncompatibleIndexActions({
      incompatibleIndices: loadedIndices,
      isLoading: !hasLoaded,
      isError: false,
      refetch,
      canArchive,
    });

  // Suppress archive actions while an archive job runs — the backend archive job is single-concurrency.
  const archiveActionsAvailable = canArchive && !isArchiveJobRunning;

  const handleDataLoaded = useCallback((data: IncompatibleIndicesResponse) => {
    setLoadedIndices(data.list);
    setHasLoaded(true);
  }, []);

  const columnRenderers = useMemo(
    () => createColumnRenderers(pendingIndexStatuses, archivedIndexNames),
    [pendingIndexStatuses, archivedIndexNames],
  );

  const renderActions = useCallback(
    (index: IncompatibleIndexRow) => {
      const pendingStatus = pendingIndexStatuses.get(index.index_name);
      const isArchived =
        pendingStatus?.state !== 'archiving' &&
        (archivedIndexNames.has(index.index_name) || pendingStatus?.state === 'archived');

      return (
        <IncompatibleIndexTableActions
          index={index}
          canArchive={archiveActionsAvailable}
          pendingStatus={pendingStatus}
          isArchived={isArchived}
          addArchiveDeleteAction={addArchiveDeleteAction}
          refetchClusterJobs={refetchClusterJobs}
          refetch={refetch}
        />
      );
    },
    [pendingIndexStatuses, archivedIndexNames, archiveActionsAvailable, addArchiveDeleteAction, refetchClusterJobs, refetch],
  );

  const bulkSelection = useMemo(
    () => ({
      actions: (
        <IncompatibleIndicesBulkActions
          indices={loadedIndices}
          canArchive={archiveActionsAvailable}
          pendingIndexStatuses={pendingIndexStatuses}
          archivedIndexNames={archivedIndexNames}
          refetch={refetch}
        />
      ),
    }),
    [loadedIndices, archiveActionsAvailable, pendingIndexStatuses, archivedIndexNames, refetch],
  );

  return (
    <>
      <Heading>Incompatible indices</Heading>
      <PaginatedEntityTable<IncompatibleIndexRow>
        humanName="incompatible indices"
        tableLayout={TABLE_LAYOUT}
        fetchEntities={fetchIncompatibleIndices}
        keyFn={incompatibleIndicesKeyFn}
        columnRenderers={columnRenderers}
        entityActions={renderActions}
        bulkSelection={bulkSelection}
        onDataLoaded={handleDataLoaded}
        entityAttributesAreCamelCase={false}
      />
    </>
  );
};

export default IncompatibleIndicesTable;
