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
import styled, { css } from 'styled-components';
import useResizeObserver from '@react-hook/resize-observer';

import { ButtonToolbar } from 'components/bootstrap';
import type { EntityBase } from 'components/common/EntityDataTable/types';
import { ACTIONS_COL_ID, CELL_PADDING } from 'components/common/EntityDataTable/Constants';
import { actionsHeaderWidthVar } from 'components/common/EntityDataTable/CSSVariables';

const AlignRight = styled.div`
  display: flex;
  justify-content: flex-end;
  height: 100%;
`;

const BackgroundFoundation = styled.div(
  ({ theme }) => css`
    background-color: ${theme.colors.global.contentBackground};
    height: 100%;
    width: var(${actionsHeaderWidthVar});
  `,
);

const Actions = styled.div<{ $isEvenRow: boolean }>(
  ({ $isEvenRow, theme }) => css`
    display: flex;
    justify-content: flex-end;
    padding: ${CELL_PADDING}px;
    background: ${$isEvenRow ? theme.colors.global.contentBackground : theme.colors.table.row.backgroundStriped};
    height: 100%;
  `,
);

const ActionCell = <Entity extends EntityBase>({
  row,
  entityActions,
  onWidthChange,
}: {
  row: Row<Entity>;
  entityActions: (entity: Entity) => React.ReactNode | undefined;
  onWidthChange: (rowId: string, width: number) => void;
}) => {
  const ref = React.useRef<HTMLDivElement>(null);

  useLayoutEffect(() => {
    if (ref.current) {
      onWidthChange(row.id, ref.current.getBoundingClientRect().width);
    }
  }, [row.id, onWidthChange]);

  useResizeObserver(ref, ({ contentRect: { width } }) => onWidthChange(row.id, width));

  return (
    <AlignRight>
      <BackgroundFoundation>
        <Actions $isEvenRow={row.index % 2 === 0}>
          <ButtonToolbar ref={ref}>{entityActions(row.original)}</ButtonToolbar>
        </Actions>
      </BackgroundFoundation>
    </AlignRight>
  );
};

const useActionsColumnDefinition = <Entity extends EntityBase>({
  colWidth,
  entityActions,
  hasRowActions,
  onWidthChange,
}: {
  colWidth: number;
  entityActions: (entity: Entity) => React.ReactNode | undefined;
  hasRowActions: boolean;
  minWidth: number;
  onWidthChange: (colId: string, width: number) => void;
}) => {
  const columnHelper = createColumnHelper<Entity>();

  const cell = useCallback(
    ({ row }: { row: Row<Entity> }) =>
      entityActions ? (
        <ActionCell<Entity> row={row} entityActions={entityActions} onWidthChange={onWidthChange} />
      ) : null,
    [entityActions, onWidthChange],
  );

  const header = useCallback(
    () => (
      <AlignRight>
        <BackgroundFoundation />
      </AlignRight>
    ),
    [],
  );

  return useMemo(
    () =>
      columnHelper.display({
        id: ACTIONS_COL_ID,
        size: colWidth,
        enableHiding: false,
        enablePinning: true,
        enableResizing: false,
        header,
        cell: hasRowActions ? cell : undefined,
      }),
    [colWidth, cell, columnHelper, hasRowActions, header],
  );
};
export default useActionsColumnDefinition;
