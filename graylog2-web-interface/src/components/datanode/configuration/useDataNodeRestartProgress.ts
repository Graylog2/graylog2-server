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
import { useEffect, useState } from 'react';

import usePermissions from 'hooks/usePermissions';

import useManagedConfiguration from './useManagedConfiguration';

import { useCurrentRollingRestart } from '../opensearch-upgrade/hooks/useOpenSearchRollingRestart';
import { isRollingRestartActive, isRollingRestartPaused } from '../opensearch-upgrade/rollingRestartTypes';

// A Data Node reports that it restarts shortly after being asked to. Until then the request itself counts.
const REQUEST_GRACE_PERIOD = 30000;

/**
 * Whether the Data Nodes shown on a configuration page are being restarted right now, so the page can refuse to
 * restart them again and show how far the restart got. The state comes from the server, so it also covers restarts
 * requested in another browser tab or by another user.
 */
const useDataNodeRestartProgress = (nodeId: string | null) => {
  const { isPermitted } = usePermissions();
  const { data: configuration } = useManagedConfiguration(nodeId, isPermitted('clusterconfigentry:read'));
  const { data: rollingRestart } = useCurrentRollingRestart(isPermitted('datanode:read'));
  const [restartRequested, setRestartRequested] = useState(false);

  const restartingNodes = configuration?.restarting_nodes ?? [];
  const isNodeRestarting = nodeId === null ? restartingNodes.length > 0 : restartingNodes.includes(nodeId);
  const rollingRestartActive = isRollingRestartActive(rollingRestart);

  useEffect(() => {
    if (!restartRequested) return undefined;
    const timeout = setTimeout(() => setRestartRequested(false), REQUEST_GRACE_PERIOD);

    return () => clearTimeout(timeout);
  }, [restartRequested]);

  const isRestarting = restartRequested || isNodeRestarting || rollingRestartActive;
  const isPaused = rollingRestartActive && isRollingRestartPaused(rollingRestart?.data?.sm_state);

  const progress = (() => {
    if (rollingRestartActive) {
      const nodes = rollingRestart?.data?.nodes ?? [];
      const done = nodes.filter(({ status }) => status === 'COMPLETED' || status === 'SKIPPED').length;
      const current = nodes.find(({ status }) => status === 'RESTARTING' || status === 'STARTED');

      if (isPaused) {
        return `The rolling restart is paused after ${done} of ${nodes.length} Data Nodes, because the cluster did not return to green health in time.`;
      }

      return `Restarting the Data Nodes one after another: ${done} of ${nodes.length} done${
        current ? `, now restarting ${current.hostname}` : ''
      }.`;
    }
    if (nodeId !== null) return 'Restarting this Data Node. This can take a few minutes.';
    if (restartingNodes.length > 0) {
      return `Restarting ${restartingNodes.length} Data Node${restartingNodes.length === 1 ? '' : 's'}.`;
    }

    return 'Restarting the Data Nodes.';
  })();

  return {
    isRestarting,
    isPaused,
    progress: isRestarting ? progress : undefined,
    markRestartRequested: () => setRestartRequested(true),
  };
};

export default useDataNodeRestartProgress;
