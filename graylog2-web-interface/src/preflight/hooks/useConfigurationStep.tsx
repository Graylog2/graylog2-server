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
import type FetchError from 'logic/errors/FetchError';

import useDataNodesCA from './useDataNodesCA';

const configurationStep = (
  dataNodes: DataNodes,
  dataNodesCA: DataNodesCA,
  renewalPolicy: RenewalPolicy,
  isSkippingProvisioning: boolean,
) => {
  if (!dataNodesCA) {
    return CONFIGURATION_STEPS.CA_CONFIGURATION.key;
  }

  if (!renewalPolicy) {
    return CONFIGURATION_STEPS.RENEWAL_POLICY_CONFIGURATION.key;
  }

  const finishedProvisioning = !!dataNodes?.length && !dataNodes.some((dataNode) => dataNode.status !== DATA_NODES_STATUS.CONNECTED);

  if (!finishedProvisioning && !isSkippingProvisioning) {
    return CONFIGURATION_STEPS.CERTIFICATE_PROVISIONING.key;
  }

  return CONFIGURATION_STEPS.CONFIGURATION_FINISHED.key;
};

type Props = {
  isSkippingProvisioning: boolean
}

type Result = {
  step: ConfigurationStep | null,
  isLoading: boolean,
  errors: Array<{ entityName: string, error: FetchError}> | null
}

const useConfigurationStep = ({ isSkippingProvisioning }: Props): Result => {
  const { data: dataNodes, isInitialLoading: isLoadingDataNodes, error: dataNodesError } = useDataNodes();
  const { data: dataNodesCA, isInitialLoading: isLoadingCAStatus, error: caError } = useDataNodesCA();
  const { data: renewalPolicy, isInitialLoading: isLoadingRenewalPolicy, error: renewalPolicyError } = useRenewalPolicy();
  const step = configurationStep(dataNodes, dataNodesCA, renewalPolicy, isSkippingProvisioning);

  return useMemo(() => {
    if (dataNodesError || caError || renewalPolicyError) {
      const errors = [
        { entityName: 'data nodes', error: dataNodesError },
        { entityName: 'certificate authority', error: caError },
        { entityName: 'renewal policy', error: renewalPolicyError },
      ].filter(({ error }) => !!error);

      return ({ isLoading: false, step: null, errors });
    }

    if (isLoadingDataNodes || isLoadingCAStatus || isLoadingRenewalPolicy) {
      return ({ isLoading: true, step: null, errors: null });
    }

    return ({
      isLoading: false,
      step,
      errors: null,
    });
  }, [
    caError,
    dataNodesError,
    isLoadingCAStatus,
    isLoadingDataNodes,
    isLoadingRenewalPolicy,
    renewalPolicyError,
    step,
  ]);
};

export default useConfigurationStep;
