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
import React, { useState } from 'react';
import styled from 'styled-components';

import { ConfirmDialog } from 'components/common';
import { Alert, Button } from 'components/bootstrap';

import useMongodbProfilingToggle from './useMongodbProfilingToggle';

const AlertContent = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
`;

const MessageBlock = styled.div`
  flex: 1 1 420px;
  min-width: 0;

  p {
    margin: 0;
  }

  p + p {
    margin-top: 4px;
  }
`;

const ActionBlock = styled.div`
  flex: 0 0 auto;
  margin-left: auto;
`;

const MongodbProfilingAction = () => {
  const [showProfilingDialog, setShowProfilingDialog] = useState(false);
  const { action, state, profilingStatusByLevel, isTogglingProfiling, runToggleAction } = useMongodbProfilingToggle();

  const enablingProfiling = action === 'enable';
  const profilingActionLabel = enablingProfiling ? 'Enable Profiling' : 'Disable Profiling';
  const profilingActionLoadingLabel = enablingProfiling ? 'Enabling...' : 'Disabling...';
  const profilingActionButtonLabel = isTogglingProfiling ? profilingActionLoadingLabel : profilingActionLabel;
  const profilingActionTitle = enablingProfiling
    ? 'Set profiling to Slow Ops on all MongoDB nodes'
    : 'Disable profiling on all MongoDB nodes';
  const offCount = profilingStatusByLevel?.OFF ?? 0;
  const slowOpsCount = profilingStatusByLevel?.SLOW_OPS ?? 0;
  const allCount = profilingStatusByLevel?.ALL ?? 0;
  const enabledCount = slowOpsCount + allCount;
  const totalNodeCount = offCount + slowOpsCount + allCount;
  const profiledNodesSummary = `${enabledCount}/${totalNodeCount} nodes profiled`;
  const distributionSummary = `OFF ${offCount}, SLOW_OPS ${slowOpsCount}, ALL ${allCount}`;
  const profilingStatusSummaryByState = {
    off: `Profiling is off for all MongoDB nodes (${profiledNodesSummary}). Enable it to collect slow-query diagnostics.`,
    mixed:
      `Profiling differs across MongoDB nodes (${distributionSummary}). Enable profiling to make diagnostics consistent.`,
    enabled:
      `Profiling is on for all MongoDB nodes (${profiledNodesSummary}). Disable it after troubleshooting to reduce MongoDB overhead.`,
    unknown: totalNodeCount > 0
      ? `Profiling state is unavailable (${distributionSummary}). You can still update it if needed.`
      : 'Profiling state is unavailable. You can still update it if needed.',
  } as const;
  const profilingStatusSummary = profilingStatusSummaryByState[state];

  const onConfirmProfilingAction = async () => {
    const actionWasSuccessful = await runToggleAction();

    if (actionWasSuccessful) {
      setShowProfilingDialog(false);
    }
  };

  const onProfilingActionClick = async () => {
    if (enablingProfiling) {
      setShowProfilingDialog(true);

      return;
    }

    await runToggleAction();
  };

  return (
    <>
      <Alert bsStyle="info">
        <AlertContent>
          <MessageBlock>
            <p>MongoDB profiling helps identify slow queries and troubleshoot performance issues across cluster nodes.</p>
            <p>{profilingStatusSummary}</p>
          </MessageBlock>
          <ActionBlock>
            <Button
              bsSize="xsmall"
              bsStyle="primary"
              title={profilingActionTitle}
              aria-label={profilingActionTitle}
              onClick={onProfilingActionClick}
              disabled={isTogglingProfiling}>
              {profilingActionButtonLabel}
            </Button>
          </ActionBlock>
        </AlertContent>
      </Alert>
      {showProfilingDialog && enablingProfiling && (
        <ConfirmDialog
          show
          title={profilingActionLabel}
          btnConfirmText={profilingActionLabel}
          submitLoadingText="Enabling profiling..."
          isAsyncSubmit
          isSubmitting={isTogglingProfiling}
          onConfirm={onConfirmProfilingAction}
          onCancel={() => setShowProfilingDialog(false)}>
          Enable <b>Slow Ops</b> profiling (level 1, 100ms threshold) on all MongoDB nodes? It applies immediately
          without restart, but resets after restart unless configured at startup.
        </ConfirmDialog>
      )}
    </>
  );
};

export default MongodbProfilingAction;
