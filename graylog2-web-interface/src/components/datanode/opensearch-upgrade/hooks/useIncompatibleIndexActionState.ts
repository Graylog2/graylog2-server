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
import { useQueryClient } from '@tanstack/react-query';

import useCanArchive from 'components/indices/hooks/useCanArchive';

import useArchivedIndexNames from './useArchivedIndexNames';
import usePendingIncompatibleIndexActions from './usePendingIncompatibleIndexActions';

import { INCOMPATIBLE_INDICES_QUERY_KEY } from '../fetchIncompatibleIndices';
import type { IncompatibleIndexRow } from '../fetchIncompatibleIndices';
import type { IncompatibleIndicesContextValue } from '../IncompatibleIndicesContext';

type Params = {
  trackedIndices: Array<IncompatibleIndexRow>;
  isLoading: boolean;
};

const useIncompatibleIndexActionState = ({ trackedIndices, isLoading }: Params): IncompatibleIndicesContextValue => {
  const queryClient = useQueryClient();
  const canArchive = useCanArchive();
  const refetch = () => queryClient.invalidateQueries({ queryKey: INCOMPATIBLE_INDICES_QUERY_KEY });

  const archivedIndexNames = useArchivedIndexNames(
    trackedIndices.map((index) => index.index_name),
    canArchive,
  );
  const { pendingIndexStatuses, addArchiveDeleteAction, addReindexAction, isArchiveJobRunning, refetchClusterJobs } =
    usePendingIncompatibleIndexActions({ incompatibleIndices: trackedIndices, isLoading, isError: false, refetch, canArchive });

  return {
    archiveActionsAvailable: canArchive && !isArchiveJobRunning,
    archivedIndexNames,
    pendingIndexStatuses,
    addArchiveDeleteAction,
    addReindexAction,
    refetchClusterJobs,
    refetch,
  };
};

export default useIncompatibleIndexActionState;
