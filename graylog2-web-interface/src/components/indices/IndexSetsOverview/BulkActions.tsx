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

import { MenuItem } from 'components/bootstrap';
import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import type { ProfileAction } from 'components/indices/IndexSetFieldTypes/profileTargets';
import { appliedIndexSetIds } from 'components/indices/IndexSetFieldTypes/profileChangeResult';
import type { ProfileChangeResponse } from 'components/indices/IndexSetFieldTypes/types';
import usePermissions from 'hooks/usePermissions';

import type { IndexSetEntity } from './types';

export type ProfileChange = {
  action: ProfileAction;
  indexSets: Array<IndexSetEntity>;
  onChangeApplied: (response: ProfileChangeResponse) => void;
};

type Props = {
  indexSetsById: Record<string, IndexSetEntity>;
  onProfileAction: (profileChange: ProfileChange) => void;
};

const BulkActions = ({ indexSetsById, onProfileAction }: Props) => {
  const { selectedEntities, setSelectedEntities } = useSelectedEntities();
  const { isAnyPermitted } = usePermissions();
  const selectedIndexSets = selectedEntities.map((id) => indexSetsById[id]).filter(Boolean);
  const canEditSelection = isAnyPermitted(selectedEntities.map((id) => `indexsets:edit:${id}` as const));

  const openProfileAction = (action: ProfileAction) => {
    const applied = new Set<string>();
    const onChangeApplied = (response: ProfileChangeResponse) => {
      appliedIndexSetIds(response).forEach((id) => applied.add(id));
      setSelectedEntities(selectedEntities.filter((id) => !applied.has(id)));
    };

    onProfileAction({ action, indexSets: selectedIndexSets, onChangeApplied });
  };

  return (
    <BulkActionsDropdown>
      {canEditSelection && (
        <>
          <MenuItem onSelect={() => openProfileAction('set')}>Set field type profile</MenuItem>
          <MenuItem onSelect={() => openProfileAction('remove')}>Remove field type profile</MenuItem>
        </>
      )}
    </BulkActionsDropdown>
  );
};

export default BulkActions;
