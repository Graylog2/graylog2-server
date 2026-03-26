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
import type {
  MongodbProfilingState,
  MongodbProfilingStatusByLevel,
  MongodbProfilingToggleAction,
} from './useMongodbProfilingToggle';

type Props = {
  action: MongodbProfilingToggleAction | null;
  state: MongodbProfilingState;
  profilingStatusByLevel: MongodbProfilingStatusByLevel | undefined;
  isStatusReady: boolean;
  isTogglingProfiling: boolean;
};

export type MongodbProfilingActionView = {
  actionLabel: string;
  actionTitle: string;
  buttonLabel: string;
  enablingProfiling: boolean;
  statusSummary: string;
};

const getActionLabel = (action: MongodbProfilingToggleAction | null): string =>
  action === 'disable' ? 'Disable Profiling' : 'Enable Profiling';

const getActionLoadingLabel = (action: MongodbProfilingToggleAction | null): string =>
  action === 'disable' ? 'Disabling...' : 'Enabling...';

const getActionTitle = (isStatusReady: boolean, enablingProfiling: boolean): string => {
  if (!isStatusReady) {
    return 'Loading MongoDB profiling status';
  }

  return enablingProfiling
    ? 'Set profiling to Slow Ops on all MongoDB nodes'
    : 'Disable profiling on all MongoDB nodes';
};

const getStatusSummary = ({
  isStatusReady,
  state,
  profiledNodesSummary,
  distributionSummary,
  totalNodeCount,
}: {
  isStatusReady: boolean;
  state: MongodbProfilingState;
  profiledNodesSummary: string;
  distributionSummary: string;
  totalNodeCount: number;
}) => {
  if (!isStatusReady) {
    return 'Loading current profiling status across MongoDB nodes...';
  }

  switch (state) {
    case 'off':
      return `Profiling is off for all MongoDB nodes (${profiledNodesSummary}). Enable it to collect slow-query diagnostics.`;
    case 'mixed':
      return `Profiling differs across MongoDB nodes (${distributionSummary}). Enable profiling to make diagnostics consistent.`;
    case 'enabled':
      return `Profiling is on for all MongoDB nodes (${profiledNodesSummary}). Disable it after troubleshooting to reduce MongoDB overhead.`;
    default:
      if (totalNodeCount > 0) {
        return `Profiling state is unavailable (${distributionSummary}). You can still update it if needed.`;
      }

      return 'Profiling state is unavailable. You can still update it if needed.';
  }
};

const buildMongodbProfilingActionView = ({
  action,
  state,
  profilingStatusByLevel,
  isStatusReady,
  isTogglingProfiling,
}: Props): MongodbProfilingActionView => {
  const actionLabel = getActionLabel(action);
  const actionLoadingLabel = getActionLoadingLabel(action);
  const enablingProfiling = action === 'enable';
  const offCount = profilingStatusByLevel?.OFF ?? 0;
  const slowOpsCount = profilingStatusByLevel?.SLOW_OPS ?? 0;
  const allCount = profilingStatusByLevel?.ALL ?? 0;
  const enabledCount = slowOpsCount + allCount;
  const totalNodeCount = offCount + slowOpsCount + allCount;
  const profiledNodesSummary = `${enabledCount}/${totalNodeCount} nodes profiled`;
  const distributionSummary = `OFF ${offCount}, SLOW_OPS ${slowOpsCount}, ALL ${allCount}`;

  const statusSummary = getStatusSummary({
    isStatusReady,
    state,
    profiledNodesSummary,
    distributionSummary,
    totalNodeCount,
  });

  let buttonLabel = 'Loading status...';
  if (isStatusReady) {
    buttonLabel = isTogglingProfiling ? actionLoadingLabel : actionLabel;
  }

  const actionTitle = getActionTitle(isStatusReady, enablingProfiling);

  return {
    actionLabel,
    actionTitle,
    buttonLabel,
    enablingProfiling,
    statusSummary,
  };
};

export default buildMongodbProfilingActionView;
