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

import type { DataNodes, ConfigurationStep, DataNodesCAStatus } from 'preflight/types';
import useDataNodes from 'preflight/hooks/useDataNodes';
import { CONFIGURATION_STEPS } from 'preflight/Constants';

import useDataNodesCA from './useDataNodesCA';

const configurationStep = (_dataNodes: DataNodes, dataNodesCAStatus: DataNodesCAStatus) => {
  if (!dataNodesCAStatus.isConfigured) {
    return CONFIGURATION_STEPS.CA_CONFIGURATION.key;
  }

  // const finishedProvisioning = !dataNodes.some((dataNode) => dataNode.status !== DATA_NODES_STATUS.SIGNED.key);
  //
  // if (!finishedProvisioning) {
  //   return CONFIGURATION_STEPS.CERTIFICATE_PROVISIONING.key;
  // }

  return CONFIGURATION_STEPS.CONFIGURATION_FINISHED.key;
};

const useConfigurationStep = (): { step: ConfigurationStep | undefined, isLoading: boolean } => {
  const { data: dataNodes, isInitialLoading: isLoadingDataNodes } = useDataNodes();
  const { data: dataNodesCAStatus, isInitialLoading: isLoadingCAStatus } = useDataNodesCA();

  if (isLoadingDataNodes || isLoadingCAStatus) {
    return ({ isLoading: true, step: undefined });
  }

  return ({
    isLoading: false,
    step: configurationStep(dataNodes, dataNodesCAStatus),
  });
};

export default useConfigurationStep;
