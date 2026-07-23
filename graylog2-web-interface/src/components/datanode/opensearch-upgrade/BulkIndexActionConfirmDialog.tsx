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
import React from 'react';

import { ConfirmDialog } from 'components/common';

import type { BulkIndexActionCandidate } from './bulkIndexActions';

const BulkIndexActionConfirmDialog = ({
  bulkAction,
  isSubmitting,
  onCancel,
  onConfirm,
}: {
  bulkAction: BulkIndexActionCandidate;
  isSubmitting: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}) => {
  const indicesLabel = bulkAction.targetIndices.length === 1 ? 'index' : 'indices';

  return (
    <ConfirmDialog
      show
      title={bulkAction.confirmTitle}
      btnConfirmText={bulkAction.confirmText}
      isAsyncSubmit
      isSubmitting={isSubmitting}
      onCancel={onCancel}
      onConfirm={onConfirm}
      submitLoadingText="Working...">
      <p>
        This will {bulkAction.targetVerb} {bulkAction.targetIndices.length} incompatible {indicesLabel}.
      </p>
      <p>If an individual index fails, the remaining indices will still be processed.</p>
    </ConfirmDialog>
  );
};

export default BulkIndexActionConfirmDialog;
