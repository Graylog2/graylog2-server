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
import { useState } from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { DATA_TIERING_TYPE } from 'components/indices/data-tiering';
import { ButtonGroup, DropdownButton, MenuItem } from 'components/bootstrap';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import IndexSetMaintenanceDialog from 'components/indices/IndexSetMaintenanceDialog';
import IncreaseFieldLimitDialog from 'components/indices/IncreaseFieldLimitDialog';
import { isFieldLimitAllowed } from 'components/indices/helpers/fieldLimit';
import usePermissions from 'hooks/usePermissions';
import AppConfig from 'util/AppConfig';
import type { IndexSetMaintenanceAction } from 'components/indices/IndexSetMaintenanceDialog';

type Props = {
  indexSet: IndexSet;
  indexSetId: string;
};

const IndicesMaintenanceDropdown = ({ indexSet, indexSetId }: Props) => {
  const dataTieringPlugin = PluginStore.exports('dataTiering').find(
    (plugin) => plugin.type === DATA_TIERING_TYPE.HOT_WARM,
  );

  const { isPermitted } = usePermissions();
  const [pendingAction, setPendingAction] = useState<IndexSetMaintenanceAction | null>(null);
  const [showFieldLimitDialog, setShowFieldLimitDialog] = useState(false);
  const canRaiseFieldLimit =
    !AppConfig.isCloud() &&
    !!indexSet?.writable &&
    isFieldLimitAllowed(indexSet.index_template_type) &&
    isPermitted(`indexsets:edit:${indexSetId}`) &&
    isPermitted('deflector:cycle');

  return (
    <>
      <ButtonGroup>
        <DropdownButton bsStyle="info" title="Maintenance" id="indices-maintenance-actions" pullRight>
          <MenuItem eventKey="1" onClick={() => setPendingAction('recalculateIndexRanges')}>
            Recalculate index ranges
          </MenuItem>
          {indexSet?.writable && (
            <MenuItem eventKey="2" onClick={() => setPendingAction('rotateActiveWriteIndex')}>
              Rotate active write index
            </MenuItem>
          )}
          {canRaiseFieldLimit && (
            <MenuItem eventKey="4" onClick={() => setShowFieldLimitDialog(true)}>
              Raise max field count and rotate
            </MenuItem>
          )}
          {indexSet?.data_tiering_status?.has_failed_snapshot && dataTieringPlugin && (
            <dataTieringPlugin.DeleteFailedSnapshotMenuItem eventKey="3" indexSetId={indexSetId} />
          )}
        </DropdownButton>
      </ButtonGroup>
      {showFieldLimitDialog && (
        <IncreaseFieldLimitDialog indexSet={indexSet} onClose={() => setShowFieldLimitDialog(false)} />
      )}
      {pendingAction && (
        <IndexSetMaintenanceDialog
          action={pendingAction}
          indexSetId={indexSetId}
          onClose={() => setPendingAction(null)}
        />
      )}
    </>
  );
};

export default IndicesMaintenanceDropdown;
