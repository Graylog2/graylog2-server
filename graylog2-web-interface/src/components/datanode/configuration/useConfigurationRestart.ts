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
import { useState } from 'react';

import usePermissions from 'hooks/usePermissions';
import UserNotification from 'util/UserNotification';

import { useRestartDataNodes } from './useManagedConfiguration';
import useDataNodeRestartProgress from './useDataNodeRestartProgress';
import { restartTargetOf } from './configurationRestart';

import { rollingRestartStartError } from '../opensearch-upgrade/hooks/useOpenSearchRollingRestart';

const useConfigurationRestart = (nodeId: string | null) => {
  const { isPermitted } = usePermissions();
  const { restartDataNodes } = useRestartDataNodes(nodeId);
  const { isRestarting, isPaused, progress, markRestartRequested } = useDataNodeRestartProgress(nodeId);
  const [forceRestartChecks, setForceRestartChecks] = useState<Array<string>>([]);
  const [isForcingRestart, setIsForcingRestart] = useState(false);
  const restartTarget = restartTargetOf(nodeId);

  const restart = async (force = false) => {
    try {
      await restartDataNodes(force);
      markRestartRequested();
      UserNotification.success(`Configuration saved. Restarting ${restartTarget}.`);
    } catch (error) {
      const startError = rollingRestartStartError(error);

      if (!force && nodeId === null && startError.canRetryWithForce) {
        setForceRestartChecks(startError.failedChecks);

        return;
      }

      UserNotification.error(startError.message, `Configuration saved, but ${restartTarget} could not be restarted`);
    }
  };

  const confirmForceRestart = async () => {
    setIsForcingRestart(true);
    try {
      await restart(true);
    } finally {
      setIsForcingRestart(false);
      setForceRestartChecks([]);
    }
  };

  return {
    canRestart: isPermitted('datanode:restart'),
    isRestarting,
    isRestartPaused: isPaused,
    restartProgress: progress,
    restart,
    forceRestart: {
      failedChecks: forceRestartChecks,
      isSubmitting: isForcingRestart,
      confirm: confirmForceRestart,
      cancel: () => setForceRestartChecks([]),
    },
  };
};

export default useConfigurationRestart;
