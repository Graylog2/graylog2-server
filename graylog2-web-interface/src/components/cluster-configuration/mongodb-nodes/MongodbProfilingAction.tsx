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
import buildMongodbProfilingActionView from './buildMongodbProfilingActionView';

const AlertContent = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacings.sm};
  flex-wrap: wrap;
`;

const MessageBlock = styled.div`
  flex: 1 1 420px;
  min-width: 0;

  p {
    margin: 0;
  }

  p + p {
    margin-top: ${({ theme }) => theme.spacings.xxs};
  }
`;

const ActionBlock = styled.div`
  flex: 0 0 auto;
  margin-left: auto;
`;

const MongodbProfilingAction = () => {
  const [showProfilingDialog, setShowProfilingDialog] = useState(false);
  const { action, state, profilingStatusByLevel, isStatusReady, isTogglingProfiling, runToggleAction } =
    useMongodbProfilingToggle();

  const { actionLabel, actionTitle, buttonLabel, enablingProfiling, statusSummary } = buildMongodbProfilingActionView({
    action,
    state,
    profilingStatusByLevel,
    isStatusReady,
    isTogglingProfiling,
  });

  const onConfirmProfilingAction = async () => {
    const actionWasSuccessful = await runToggleAction();

    if (actionWasSuccessful) {
      setShowProfilingDialog(false);
    }
  };

  const onProfilingActionClick = async () => {
    if (!isStatusReady) {
      return;
    }

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
            <p>
              MongoDB profiling helps identify slow queries and troubleshoot performance issues across cluster nodes.
            </p>
            <p>{statusSummary}</p>
          </MessageBlock>
          <ActionBlock>
            <Button
              bsSize="xsmall"
              bsStyle="primary"
              title={actionTitle}
              aria-label={actionTitle}
              onClick={onProfilingActionClick}
              disabled={isTogglingProfiling || !isStatusReady}>
              {buttonLabel}
            </Button>
          </ActionBlock>
        </AlertContent>
      </Alert>
      {showProfilingDialog && isStatusReady && enablingProfiling && (
        <ConfirmDialog
          show
          title={actionLabel}
          btnConfirmText={actionLabel}
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
