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
import { useRef, useState } from 'react';

import { ButtonToolbar, MenuItem, DeleteMenuItem } from 'components/bootstrap';
import { IconButton, IfPermitted, LinkContainer } from 'components/common';
import { MoreActions } from 'components/common/EntityDataTable';
import IndexSetDeletionForm from 'components/indices/IndexSetDeletionForm';
import IndexSetMaintenanceDialog from 'components/indices/IndexSetMaintenanceDialog';
import type { IndexSetMaintenanceAction } from 'components/indices/IndexSetMaintenanceDialog';
import useHasTypeMappingPermission from 'hooks/useHasTypeMappingPermission';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import Routes from 'routing/Routes';
import useHistory from 'routing/useHistory';
import type { IndexSet } from 'stores/indices/IndexSetsStore';

import useIndexSetMutations from './hooks/useIndexSetMutations';
import type { IndexSetEntity } from './types';

type Props = {
  indexSet: IndexSetEntity;
};

const IndexSetActions = ({ indexSet }: Props) => {
  const history = useHistory();
  const sendTelemetry = useSendTelemetry('index-sets');
  const hasMappingPermission = useHasTypeMappingPermission();
  const { setDefaultIndexSet, deleteIndexSet } = useIndexSetMutations();
  const deletionFormRef = useRef<IndexSetDeletionForm>(null);
  const [pendingAction, setPendingAction] = useState<IndexSetMaintenanceAction | null>(null);

  const onSetDefault = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INDICES.INDEX_SET_DEFAULT_SET, {
      app_action_value: 'set-default-index-set',
    });

    setDefaultIndexSet(indexSet);
  };

  const onDelete = (_indexSet: IndexSet, deleteIndices: boolean) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INDICES.INDEX_SET_DELETED, {
      app_action_value: 'delete-index-set',
    });

    deleteIndexSet({ indexSet, deleteIndices });
  };

  return (
    <ButtonToolbar>
      <IfPermitted permissions="searches:relative">
        <LinkContainer to={Routes.search(`_index:${indexSet.index_prefix}_*`, undefined)}>
          <IconButton name="search" title={`Search in index set ${indexSet.title}`} bsStyle="default" size="xsmall" />
        </LinkContainer>
      </IfPermitted>
      <MoreActions>
        <IfPermitted permissions={`indexsets:edit:${indexSet.id}`}>
          <MenuItem onSelect={() => history.push(Routes.SYSTEM.INDEX_SETS.CONFIGURATION(indexSet.id))}>Edit</MenuItem>
        </IfPermitted>
        {hasMappingPermission && (
          <MenuItem onSelect={() => history.push(Routes.SYSTEM.INDEX_SETS.FIELD_TYPES(indexSet.id))}>
            Configure field types
          </MenuItem>
        )}
        <IfPermitted permissions="indexranges:rebuild">
          <MenuItem onSelect={() => setPendingAction('recalculateIndexRanges')}>Recalculate index ranges</MenuItem>
        </IfPermitted>
        {indexSet.writable && (
          <IfPermitted permissions="deflector:cycle">
            <MenuItem onSelect={() => setPendingAction('rotateActiveWriteIndex')}>Rotate active write index</MenuItem>
          </IfPermitted>
        )}
        <IfPermitted permissions={`indexsets:edit:${indexSet.id}`}>
          <MenuItem onSelect={onSetDefault} disabled={!indexSet.can_be_default || indexSet.default}>
            Set as default
          </MenuItem>
        </IfPermitted>
        <IfPermitted permissions={`indexsets:delete:${indexSet.id}`}>
          <MenuItem divider />
          <DeleteMenuItem onSelect={() => deletionFormRef.current?.open()} />
        </IfPermitted>
      </MoreActions>
      <IndexSetDeletionForm ref={deletionFormRef} indexSet={indexSet} onDelete={onDelete} />
      {pendingAction && (
        <IndexSetMaintenanceDialog
          action={pendingAction}
          indexSetId={indexSet.id}
          onClose={() => setPendingAction(null)}
        />
      )}
    </ButtonToolbar>
  );
};

export default IndexSetActions;
