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
import type { TFunction } from 'i18next';

import type {
  MongodbProfilingState,
  MongodbProfilingStatusByLevel,
  MongodbProfilingToggleAction,
} from './useMongodbProfilingToggle';

type Props = {
  action: MongodbProfilingToggleAction | null;
  state: MongodbProfilingState;
  profilingStatusByLevel: MongodbProfilingStatusByLevel | undefined;
  slowMs: number | undefined;
  isStatusReady: boolean;
  isTogglingProfiling: boolean;
  /** `t` of the `system` namespace */
  t: TFunction;
};

export type MongodbProfilingActionView = {
  actionLabel: string;
  actionTitle: string;
  buttonLabel: string;
  enablingProfiling: boolean;
  statusSummary: string;
  enableThresholdLabel: string;
};

// slowMs reflects whatever the MongoDB nodes are already configured with -- Graylog no longer sets it when
// enabling profiling (doing so requires elevated privileges as of MongoDB 8.0.29 / SERVER-130198), so it's
// only known once the status call actually reports it.
const getEnableThresholdLabel = (slowMs: number | undefined, t: TFunction): string =>
  slowMs === undefined
    ? t('mongodb_profiling_action.threshold_level')
    : t('mongodb_profiling_action.threshold_level_with_ms', { slowMs });

const getActionLabel = (action: MongodbProfilingToggleAction | null, t: TFunction): string =>
  action === 'disable'
    ? t('mongodb_profiling_action.disable_profiling')
    : t('mongodb_profiling_action.enable_profiling');

const getActionLoadingLabel = (action: MongodbProfilingToggleAction | null, t: TFunction): string =>
  action === 'disable' ? t('mongodb_profiling_action.disabling') : t('mongodb_profiling_action.enabling');

const getActionTitle = (isStatusReady: boolean, enablingProfiling: boolean, t: TFunction): string => {
  if (!isStatusReady) {
    return t('mongodb_profiling_action.loading_status_title');
  }

  return enablingProfiling ? t('mongodb_profiling_action.enable_title') : t('mongodb_profiling_action.disable_title');
};

const getStatusSummary = ({
  isStatusReady,
  state,
  profiledNodesSummary,
  distributionSummary,
  totalNodeCount,
  t,
}: {
  isStatusReady: boolean;
  state: MongodbProfilingState;
  profiledNodesSummary: string;
  distributionSummary: string;
  totalNodeCount: number;
  t: TFunction;
}) => {
  if (!isStatusReady) {
    return t('mongodb_profiling_action.status_loading');
  }

  switch (state) {
    case 'off':
      return t('mongodb_profiling_action.status_off', { summary: profiledNodesSummary });
    case 'mixed':
      return t('mongodb_profiling_action.status_mixed', { summary: distributionSummary });
    case 'enabled':
      return t('mongodb_profiling_action.status_enabled', { summary: profiledNodesSummary });
    default:
      if (totalNodeCount > 0) {
        return t('mongodb_profiling_action.status_unavailable_with_summary', { summary: distributionSummary });
      }

      return t('mongodb_profiling_action.status_unavailable');
  }
};

const buildMongodbProfilingActionView = ({
  action,
  state,
  profilingStatusByLevel,
  slowMs,
  isStatusReady,
  isTogglingProfiling,
  t,
}: Props): MongodbProfilingActionView => {
  const actionLabel = getActionLabel(action, t);
  const actionLoadingLabel = getActionLoadingLabel(action, t);
  const enablingProfiling = action === 'enable';
  const offCount = profilingStatusByLevel?.OFF ?? 0;
  const slowOpsCount = profilingStatusByLevel?.SLOW_OPS ?? 0;
  const allCount = profilingStatusByLevel?.ALL ?? 0;
  const enabledCount = slowOpsCount + allCount;
  const totalNodeCount = offCount + slowOpsCount + allCount;
  const profiledNodesSummary = t('mongodb_profiling_action.nodes_profiled', {
    count: totalNodeCount,
    enabledCount,
  });
  const distributionSummary = t('mongodb_profiling_action.distribution', {
    off: offCount,
    slowOps: slowOpsCount,
    all: allCount,
  });

  const statusSummary = getStatusSummary({
    isStatusReady,
    state,
    profiledNodesSummary,
    distributionSummary,
    totalNodeCount,
    t,
  });

  let buttonLabel = t('mongodb_profiling_action.loading_status');
  if (isStatusReady) {
    buttonLabel = isTogglingProfiling ? actionLoadingLabel : actionLabel;
  }

  const actionTitle = getActionTitle(isStatusReady, enablingProfiling, t);
  const enableThresholdLabel = getEnableThresholdLabel(slowMs, t);

  return {
    actionLabel,
    actionTitle,
    buttonLabel,
    enablingProfiling,
    statusSummary,
    enableThresholdLabel,
  };
};

export default buildMongodbProfilingActionView;
