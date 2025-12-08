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
import { useCallback, useMemo, useLayoutEffect } from 'react';
import type { Row } from '@tanstack/react-table';
import { createColumnHelper } from '@tanstack/react-table';
import styled from 'styled-components';
import useResizeObserver from '@react-hook/resize-observer';

import { ButtonToolbar } from 'components/bootstrap';
import type { EntityBase } from 'components/common/EntityDataTable/types';
import { ACTIONS_COL_ID } from 'components/common/EntityDataTable/Constants';

const Actions = styled.div`
  display: flex;
  justify-content: flex-end;
`;

const ActionCell = <Entity extends EntityBase>({
  row,
  entityActions,
  onWidthChange,
}: {
  row: Row<Entity>;
  entityActions: (entity: Entity) => React.ReactNode | undefined;
  onWidthChange: (width: number) => void;
}) => {
  const ref = React.useRef<HTMLDivElement>(null);

  useLayoutEffect(() => {
    if (ref.current) {
      onWidthChange(ref.current.getBoundingClientRect().width);
    }
  }, [onWidthChange]);

  useResizeObserver(ref, ({ contentRect: { width } }) => onWidthChange(width));

  return (
    <Actions>
      <ButtonToolbar ref={ref}>{entityActions(row.original)}</ButtonToolbar>
    </Actions>
  );
};

const useActionsColumnDefinition = <Entity extends EntityBase>({
  hasRowActions,
  colWidth,
  entityActions,
  onWidthChange,
}: {
  hasRowActions: boolean;
  colWidth: number;
  entityActions: (entity: Entity) => React.ReactNode | undefined;
  onWidthChange: (width: number) => void;
}) => {
  const columnHelper = createColumnHelper<Entity>();

  const cell = useCallback(
    ({ row }: { row: Row<Entity> }) =>
      entityActions ? (
        <ActionCell<Entity> row={row} entityActions={entityActions} onWidthChange={onWidthChange} />
      ) : null,
    [entityActions, onWidthChange],
  );

  return useMemo(
    () =>
      columnHelper.display({
        id: ACTIONS_COL_ID,
        size: colWidth,
        enableHiding: false,
        enableResizing: false,
        cell: hasRowActions ? cell : undefined,
      }),
    [colWidth, cell, columnHelper, hasRowActions],
  );
};
export default useActionsColumnDefinition;
