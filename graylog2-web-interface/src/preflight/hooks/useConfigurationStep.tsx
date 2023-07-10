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

import { useMemo } from 'react';

import { CONFIGURATION_STEPS, DATA_NODES_STATUS } from 'preflight/Constants';
import type { DataNodes, ConfigurationStep, DataNodesCA, RenewalPolicy } from 'preflight/types';
import useDataNodes from 'preflight/hooks/useDataNodes';
import useRenewalPolicy from 'preflight/hooks/useRenewalPolicy';

import useDataNodesCA from './useDataNodesCA';

const configurationStep = (dataNodes: DataNodes, dataNodesCA: DataNodesCA, renewalPolicy: RenewalPolicy) => {
  if (!dataNodesCA) {
    return CONFIGURATION_STEPS.CA_CONFIGURATION.key;
  }

  if (!renewalPolicy) {
    return CONFIGURATION_STEPS.RENEWAL_POLICY_CONFIGURATION.key;
  }

  const finishedProvisioning = !dataNodes.some((dataNode) => dataNode.status !== DATA_NODES_STATUS.CONNECTED.key);

  if (!finishedProvisioning) {
    return CONFIGURATION_STEPS.CERTIFICATE_PROVISIONING.key;
  }

  return CONFIGURATION_STEPS.CONFIGURATION_FINISHED.key;
};

const useConfigurationStep = (): { step: ConfigurationStep | undefined, isLoading: boolean } => {
  const { data: dataNodes, isInitialLoading: isLoadingDataNodes } = useDataNodes();
  const { data: dataNodesCA, isInitialLoading: isLoadingCAStatus } = useDataNodesCA();
  const { data: renewalPolicy, isInitialLoading: isLoadingRenewalPolicy } = useRenewalPolicy();
  const step = configurationStep(dataNodes, dataNodesCA, renewalPolicy);

  return useMemo(() => {
    if (isLoadingDataNodes || isLoadingCAStatus || isLoadingRenewalPolicy) {
      return ({ isLoading: true, step: undefined });
    }

    return ({
      isLoading: false,
      step,
    });
  }, [isLoadingCAStatus, isLoadingDataNodes, isLoadingRenewalPolicy, step]);
};

export default useConfigurationStep;
