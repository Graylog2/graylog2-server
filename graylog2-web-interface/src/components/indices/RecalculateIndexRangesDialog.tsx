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
import { recalculateIndexRanges } from 'components/indices/helpers/indexSetMaintenanceActions';

type Props = {
  indexSetId: string;
  onClose: () => void;
};

const RecalculateIndexRangesDialog = ({ indexSetId, onClose }: Props) => {
  const onConfirm = () => {
    recalculateIndexRanges(indexSetId);
    onClose();
  };

  return (
    <ConfirmDialog
      show
      title="Recalculate index ranges"
      btnConfirmText="Recalculate"
      onConfirm={onConfirm}
      onCancel={onClose}>
      This will recalculate index ranges for this index set using a background system job. Do you want to proceed?
    </ConfirmDialog>
  );
};

export default RecalculateIndexRangesDialog;
