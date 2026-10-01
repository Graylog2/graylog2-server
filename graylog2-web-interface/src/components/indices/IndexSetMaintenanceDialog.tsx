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

import ConfirmDialog from 'components/common/ConfirmDialog';
import { cycleActiveWriteIndex, recalculateIndexRanges } from 'components/indices/helpers/indexSetMaintenanceActions';

export type IndexSetMaintenanceAction = 'recalculateIndexRanges' | 'rotateActiveWriteIndex';

const ACTIONS: {
  [action in IndexSetMaintenanceAction]: {
    title: string;
    confirmText: string;
    message: string;
    run: (indexSetId: string) => unknown;
  };
} = {
  recalculateIndexRanges: {
    title: 'Recalculate index ranges',
    confirmText: 'Recalculate',
    message:
      'This will recalculate index ranges for this index set using a background system job. Do you want to proceed?',
    run: recalculateIndexRanges,
  },
  rotateActiveWriteIndex: {
    title: 'Rotate active write index',
    confirmText: 'Rotate',
    message: 'This will manually cycle the current active write index on this index set. Do you want to proceed?',
    run: cycleActiveWriteIndex,
  },
};

type Props = {
  action: IndexSetMaintenanceAction;
  indexSetId: string;
  onClose: () => void;
};

const IndexSetMaintenanceDialog = ({ action, indexSetId, onClose }: Props) => {
  const { title, confirmText, message, run } = ACTIONS[action];

  const onConfirm = () => {
    run(indexSetId);
    onClose();
  };

  return (
    <ConfirmDialog show title={title} btnConfirmText={confirmText} onConfirm={onConfirm} onCancel={onClose}>
      {message}
    </ConfirmDialog>
  );
};

export default IndexSetMaintenanceDialog;
