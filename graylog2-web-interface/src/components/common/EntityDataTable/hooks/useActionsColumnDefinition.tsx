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
import { useCallback, useMemo } from 'react';
import type { Row } from '@tanstack/react-table';
import { createColumnHelper } from '@tanstack/react-table';
import styled from 'styled-components';

import { ButtonToolbar } from 'components/bootstrap';
import type { EntityBase } from 'components/common/EntityDataTable/types';
import { ACTIONS_COL_ID } from 'components/common/EntityDataTable/Constants';

const ActionsHead = styled.div`
  text-align: right;
`;

const Actions = styled(ButtonToolbar)`
  justify-content: flex-end;
`;

const ActionsHeader = () => <ActionsHead>Actions</ActionsHead>;

const useActionsColumnDefinition = <Entity extends EntityBase>(
  displayActionsCol: boolean,
  actionsColWidth: number,
  entityActions: (entity: Entity) => React.ReactNode | undefined,
  colRef: React.MutableRefObject<HTMLDivElement>,
) => {
  const columnHelper = createColumnHelper<Entity>();

  const cell = useCallback(
    ({ row }: { row: Row<Entity> }) => (
      <div ref={colRef}>
        <Actions>{entityActions(row.original)}</Actions>
      </div>
    ),
    [colRef, entityActions],
  );

  return useMemo(
    () =>
      columnHelper.display({
        id: ACTIONS_COL_ID,
        size: actionsColWidth,
        header: displayActionsCol ? ActionsHeader : undefined,
        enableHiding: false,
        enableResizing: false,
        cell: displayActionsCol ? cell : undefined,
      }),
    [actionsColWidth, cell, columnHelper, displayActionsCol],
  );
};
export default useActionsColumnDefinition;
