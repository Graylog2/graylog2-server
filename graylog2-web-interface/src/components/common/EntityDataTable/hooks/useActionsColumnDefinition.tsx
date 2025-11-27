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
import styled, { css } from 'styled-components';
import chroma from 'chroma-js';

import { ButtonToolbar } from 'components/bootstrap';
import type { EntityBase } from 'components/common/EntityDataTable/types';
import { ACTIONS_COL_ID } from 'components/common/EntityDataTable/Constants';

function flattenOver(bgColor, rgbaColor) {
  const fg = chroma(rgbaColor);
  const bg = chroma(bgColor);

  const alpha = fg.alpha(); // e.g. 0.1
  const fgOpaque = fg.alpha(1); // same RGB, full alpha

  // bg * (1 - a) + fg * a  → equivalent to your rgba over bg
  return chroma.mix(bg, fgOpaque, alpha, 'rgb').hex();
}

const ActionsHead = styled.div`
  display: flex;
  justify-content: flex-end;
  flex: 1;
`;

const ActionsHeadInner = styled.div<{ $width: number }>(
  ({ $width }) => `
  width: ${$width}px;
`,
);

const Actions = styled(ButtonToolbar)`
  justify-content: flex-end;
`;

const ActionsInner = styled(ButtonToolbar)<{ $isEvenRow: boolean }>(
  ({ theme, $isEvenRow }) => css`
    background-color: ${flattenOver(
      theme.colors.global.contentBackground,
      $isEvenRow ? theme.colors.global.contentBackground : theme.colors.table.row.backgroundStriped,
    )};
  `,
);

const useActionsColumnDefinition = <Entity extends EntityBase>(
  displayActionsCol: boolean,
  actionsColWidth: number,
  entityActions: (entity: Entity) => React.ReactNode | undefined,
  minActionsCellWidth: number,
) => {
  const columnHelper = createColumnHelper<Entity>();

  const cell = useCallback(
    ({ row }: { row: Row<Entity> }) => (
      <Actions>
        <ActionsInner $isEvenRow={row.index % 2 === 0}>{entityActions(row.original)}</ActionsInner>
      </Actions>
    ),
    [entityActions],
  );

  const header = useCallback(
    () => (
      <ActionsHead>
        <ActionsHeadInner $width={minActionsCellWidth} />
      </ActionsHead>
    ),
    [minActionsCellWidth],
  );

  return useMemo(
    () =>
      columnHelper.display({
        id: ACTIONS_COL_ID,
        size: actionsColWidth,
        header: displayActionsCol ? header : undefined,
        enableHiding: false,
        enablePinning: true,
        enableResizing: false,
        cell: displayActionsCol ? cell : undefined,
      }),
    [actionsColWidth, cell, columnHelper, displayActionsCol, header],
  );
};
export default useActionsColumnDefinition;
