/*
// /!*
//  * Copyright (C) 2020 Graylog, Inc.
//  *
//  * This program is free software: you can redistribute it and/or modify
//  * it under the terms of the Server Side Public License, version 1,
//  * as published by MongoDB, Inc.
//  *
//  * This program is distributed in the hope that it will be useful,
//  * but WITHOUT ANY WARRANTY; without even the implied warranty of
//  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//  * Server Side Public License for more details.
//  *
//  * You should have received a copy of the Server Side Public License
//  * along with this program. If not, see
//  * <http://www.mongodb.com/licensing/server-side-public-license>.
//  *!/
// import { useMemo } from 'react';
//
// import type { DataNodes, DataNodesCA, RenewalPolicy } from 'preflight/types';
// import { MIGRATION_STEP } from 'components/datanode/Constants';
// import useDataNodesCA from 'preflight/hooks/useDataNodesCA';
// import useDataNodes from 'components/datanode/hooks/useDataNodes';
// import useRenewalPolicy from 'preflight/hooks/useRenewalPolicy';
//
// const configurationStep = (
//   dataNodes: DataNodes,
//   dataNodesCA: DataNodesCA,
//   renewalPolicy: RenewalPolicy,
// ) => {
//   if (!dataNodesCA) {
//     return MIGRATION_STEP.CA_CONFIGURATION.key;
//   }
//
//   if (!renewalPolicy) {
//     return MIGRATION_STEP.RENEWAL_POLICY_CONFIGURATION.key;
//   }
//
//   return MIGRATION_STEP.MIGRATION_FINISHED.key;
// };
//
// const useMigrationStep = () => {
//   const { data: dataNodes, isInitialLoading: isLoadingDataNodes } = useDataNodes();
//   const { data: dataNodesCA, isInitialLoading: isLoadingCAStatus, error: caError } = useDataNodesCA();
//   const { data: renewalPolicy, isInitialLoading: isLoadingRenewalPolicy, error: renewalPolicyError } = useRenewalPolicy();
//
//   const step = configurationStep(dataNodes.elements, dataNodesCA, renewalPolicy);
//
//   return useMemo(() => ({
//     isLoading: false,
//     step,
//     errors: null,
//   }), [step]);
// };
//
// export default useMigrationStep;
*/
