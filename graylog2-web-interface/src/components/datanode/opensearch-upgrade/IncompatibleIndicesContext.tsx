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
import { createContext, useContext } from 'react';

import type { PendingIndexStatus } from './hooks/usePendingIncompatibleIndexActions';
import type { PendingArchiveTracking } from './incompatibleIndexActions';

export type IncompatibleIndicesContextValue = {
  archiveActionsAvailable: boolean;
  archivedIndexNames: ReadonlySet<string>;
  pendingIndexStatuses: Map<string, PendingIndexStatus>;
  addArchiveDeleteAction: (tracking: PendingArchiveTracking) => void;
  refetchClusterJobs?: () => void;
  refetch: () => void;
};

const IncompatibleIndicesContext = createContext<IncompatibleIndicesContextValue | undefined>(undefined);

export const useIncompatibleIndicesContext = (): IncompatibleIndicesContextValue => {
  const context = useContext(IncompatibleIndicesContext);

  if (!context) {
    throw new Error('useIncompatibleIndicesContext must be used within an IncompatibleIndicesContext.Provider');
  }

  return context;
};

export default IncompatibleIndicesContext;
