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

import { MenuItem } from 'components/bootstrap';
import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';

import { actionTargets, bulkMenu, NOT_APPLICABLE_REASON } from './actions';
import IndexActionConfirm from './IndexActionConfirm';
import type { IndexAction } from './types';
import useCanRun from './useCanRun';
import useIndexActions from './useIndexActions';
import useIndexOverview from './useIndexOverview';

// Actions permitted on at least one selected index, with how many of them each would run on.
const IndexBulkActions = () => {
  const can = useCanRun();
  const { selectedEntities, setSelectedEntities } = useSelectedEntities();
  const { indices } = useIndexOverview();
  const { runAction, isRunning } = useIndexActions();
  const [pending, setPending] = useState<IndexAction | undefined>(undefined);

  // Selected names that still exist (deleted ones drop out after a refresh).
  const selectedNames = new Set(selectedEntities);
  const selected = indices.filter((index) => selectedNames.has(index.index));
  const targets = pending ? actionTargets(pending, selected, can) : [];

  const confirm = async () => {
    try {
      await runAction({ action: pending, indices: targets });
      setSelectedEntities([]);
    } finally {
      setPending(undefined);
    }
  };

  return (
    <>
      <BulkActionsDropdown>
        {bulkMenu(selected, can).map(({ action, applicable }) => (
          <MenuItem
            key={action.key}
            disabled={applicable === 0}
            title={applicable === 0 ? NOT_APPLICABLE_REASON : undefined}
            onClick={() => setPending(action)}>
            {action.label}
            {applicable < selected.length && ` (${applicable} of ${selected.length})`}
          </MenuItem>
        ))}
      </BulkActionsDropdown>
      {pending && (
        <IndexActionConfirm
          action={pending}
          targets={targets}
          skipped={selected.length - targets.length}
          isRunning={isRunning}
          onConfirm={confirm}
          onCancel={() => setPending(undefined)}
        />
      )}
    </>
  );
};

export default IndexBulkActions;
