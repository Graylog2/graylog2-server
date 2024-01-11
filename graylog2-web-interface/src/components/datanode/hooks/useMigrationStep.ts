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

import type { DataNodes, DataNodesCA, RenewalPolicy } from 'preflight/types';
import { MIGRATION_STEP } from 'components/datanode/Constants';
import useDataNodes from 'components/datanode/hooks/useDataNodes';
import type { CompatibilityResponseType } from 'components/datanode/Types';
import useCompatibilityCheck from 'components/datanode/hooks/useCompatibilityCheck';
import useDataNodesCA from 'components/datanode/hooks/useDataNodesCA';
import useRenewalPolicy from 'components/datanode/hooks/useRenewalPolicy';

export const STEP_KEYS = ['welcome', 'compatibility-check', 'ca-configuration', 'manual-migration'];

const migrationStep = (
  compatibilityResult: CompatibilityResponseType,
  _: DataNodes,
  dataNodesCA: DataNodesCA,
  renewalPolicy: RenewalPolicy,
) => {
  if (compatibilityResult) {
    return { step: MIGRATION_STEP.COMPATIBILITY_CHECK.key, wizardStep: STEP_KEYS[1] };
  }

  if (!dataNodesCA) {
    return { step: MIGRATION_STEP.CA_CONFIGURATION.key, wizardStep: STEP_KEYS[2] };
  }

  if (!renewalPolicy) {
    return { step: MIGRATION_STEP.RENEWAL_POLICY_CONFIGURATION.key, wizardStep: STEP_KEYS[2] };
  }

  return { step: MIGRATION_STEP.MIGRATION_FINISHED.key, wizardStep: STEP_KEYS[3] };
};

const useMigrationStep = () => {
  const { data: dataNodes, isInitialLoading: isLoadingDataNodes, error: dataNodesError } = useDataNodes(undefined, undefined, false);
  const { data: dataNodesCA, isInitialLoading: isLoadingCAStatus, error: caError } = useDataNodesCA();
  const { data: renewalPolicy, isInitialLoading: isLoadingRenewalPolicy, error: renewalPolicyError } = useRenewalPolicy();
  const { data: compatibilityResult, isInitialLoading: isLoadingCompatibility, error: compatibilityCheckError } = useCompatibilityCheck({ enabled: false });

  const { step, wizardStep } = migrationStep(compatibilityResult, dataNodes.elements, dataNodesCA, renewalPolicy);

  return useMemo(() => {
    if (dataNodesError || caError || compatibilityCheckError || renewalPolicyError) {
      const errors = [
        { entityName: 'compatibility check', error: compatibilityCheckError },
        { entityName: 'data nodes', error: dataNodesError },
        { entityName: 'certificate authority', error: caError },
        { entityName: 'renewal policy', error: renewalPolicyError },
      ].filter(({ error }) => !!error);

      return ({ isLoading: false, step, errors, wizardStep });
    }

    if (isLoadingCompatibility || isLoadingDataNodes || isLoadingCAStatus || isLoadingRenewalPolicy) {
      return ({ isLoading: true, step: null, errors: null, wizardStep: STEP_KEYS[0] });
    }

    return ({
      isLoading: false,
      step,
      wizardStep,
      errors: null,
    });
  }, [
    caError,
    dataNodesError,
    compatibilityCheckError,
    isLoadingCompatibility,
    isLoadingCAStatus,
    isLoadingDataNodes,
    isLoadingRenewalPolicy,
    wizardStep,
    renewalPolicyError,
    step,
  ]);
};

export default useMigrationStep;
