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
import { useTranslation } from 'react-i18next';

import ConfirmDialog from 'components/common/ConfirmDialog';
import { cycleActiveWriteIndex, recalculateIndexRanges } from 'components/indices/helpers/indexSetMaintenanceActions';

export type IndexSetMaintenanceAction = 'recalculateIndexRanges' | 'rotateActiveWriteIndex';

const ACTIONS: {
  [action in IndexSetMaintenanceAction]: {
    translationKey: string;
    run: (indexSetId: string) => unknown;
  };
} = {
  recalculateIndexRanges: { translationKey: 'recalculate', run: recalculateIndexRanges },
  rotateActiveWriteIndex: { translationKey: 'rotate', run: cycleActiveWriteIndex },
};

type Props = {
  action: IndexSetMaintenanceAction;
  indexSetId: string;
  onClose: () => void;
};

const IndexSetMaintenanceDialog = ({ action, indexSetId, onClose }: Props) => {
  const { t } = useTranslation('indexsets');
  const { translationKey, run } = ACTIONS[action];
  const title = t(`maintenance_dialog.${translationKey}_title`);
  const confirmText = t(`maintenance_dialog.${translationKey}_confirm`);
  const message = t(`maintenance_dialog.${translationKey}_message`);

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
