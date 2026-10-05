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
import * as React from 'react';
import styled from 'styled-components';

import Alert from 'components/bootstrap/Alert';
import BootstrapModalConfirm from 'components/bootstrap/BootstrapModalConfirm';

import { NOT_APPLICABLE_REASON } from './actions';
import type { IndexAction, IndexSummary } from './types';

const MAX_LISTED = 20;

const IndexList = styled.ul`
  max-height: 240px;
  overflow-y: auto;
  font-family: monospace;
`;

const plural = (count: number) => `${count} ${count === 1 ? 'index' : 'indices'}`;

type Props = {
  action: IndexAction;
  // The indices the action applies to.
  targets: Array<IndexSummary>;
  // How many selected indices it doesn't apply to (they aren't sent).
  skipped: number;
  isRunning: boolean;
  onConfirm: () => void;
  onCancel: () => void;
};

// Asks before running an action.
const IndexActionConfirm = ({ action, targets, skipped, isRunning, onConfirm, onCancel }: Props) => (
  <BootstrapModalConfirm
    showModal
    title={`${action.label} ${plural(targets.length)}?`}
    confirmButtonText={action.label}
    isAsyncSubmit
    isSubmitting={isRunning}
    submitLoadingText={`${action.label}...`}
    onConfirm={onConfirm}
    onCancel={onCancel}>
    <>
      {action.danger ? <Alert bsStyle="danger">{action.notes}</Alert> : <p>{action.notes}</p>}
      <IndexList>
        {targets.slice(0, MAX_LISTED).map((index) => (
          <li key={index.index}>{index.index}</li>
        ))}
        {targets.length > MAX_LISTED && <li>…and {targets.length - MAX_LISTED} more</li>}
      </IndexList>
      {skipped > 0 && (
        <Alert bsStyle="info">
          {plural(skipped)} of the selection will be skipped. {NOT_APPLICABLE_REASON}
        </Alert>
      )}
    </>
  </BootstrapModalConfirm>
);

export default IndexActionConfirm;
