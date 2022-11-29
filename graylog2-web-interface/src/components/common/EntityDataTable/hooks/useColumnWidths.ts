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
import type * as React from 'react';
import { useRef, useState, useLayoutEffect, useContext } from 'react';

import type { ColumnRenderers } from 'components/common/EntityDataTable';
import WindowDimensionsContext from 'contexts/WindowDimensionsContext';
import {
  BULK_SELECT_COLUMN_WIDTH,
  DEFAULT_COL_MIN_WIDTH,
  DEFAULT_COL_WIDTH,
  CELL_PADDING,
} from 'components/common/EntityDataTable/Constants';

const calculateAvailableWidth = ({
  tableWidth,
  displayBulkSelectCol,
  displayActionsCol,
  actionsRef,
  columnsIds,
  columnRenderers,
}: {
  actionsRef: React.RefObject<HTMLDivElement>,
  columnRenderers: { [columnId: string]: { staticWidth?: number } }
  columnsIds: Array<string>,
  displayActionsCol: boolean,
  displayBulkSelectCol: boolean,
  tableWidth: number,
}) => {
  const bulkSelectColWidth = displayBulkSelectCol ? BULK_SELECT_COLUMN_WIDTH : 0;
  const actionsColWidth = displayActionsCol && actionsRef.current ? (actionsRef.current.offsetWidth + CELL_PADDING * 2) : 0;
  const staticColsWidth = columnsIds.reduce((total, id) => total + (columnRenderers[id]?.staticWidth ?? 0), 0);

  return tableWidth - bulkSelectColWidth - actionsColWidth - staticColsWidth;
};

const columnsWidth = ({
  assignableWidth,
  columnsIds,
  columnRenderers,
}: {
  assignableWidth: number,
  columnRenderers: { [columnId: string]: { staticWidth?: number, width?: number, minWidth?: number } }
  columnsIds: Array<string>,
}) => {
  const totalFlexColumns = columnsIds.reduce((total, id) => {
    const { staticWidth, width = DEFAULT_COL_WIDTH } = columnRenderers[id] ?? {};

    if (staticWidth) {
      return total;
    }

    return total + width;
  }, 0);

  const flexColWidth = assignableWidth / totalFlexColumns;

  return Object.fromEntries(columnsIds.map((id) => {
    const { staticWidth, width = DEFAULT_COL_WIDTH, minWidth = DEFAULT_COL_MIN_WIDTH } = columnRenderers[id] ?? {};
    const targetWidth = staticWidth ?? (flexColWidth * width);

    return [id, (!staticWidth && targetWidth < minWidth) ? minWidth : targetWidth];
  }));
};

const useColumnWidths = <Entity extends { id: string }>(
  {
    columnsIds,
    columnRenderers,
    displayActionsCol,
    displayBulkSelectCol,
  }: {
  columnsIds: Array<string>,
    columnRenderers: ColumnRenderers<Entity>,
    displayActionsCol: boolean,
    displayBulkSelectCol: boolean,
},
) => {
  const tableRef = useRef<HTMLTableElement>();
  const actionsRef = useRef<HTMLDivElement>();
  const [columnsWidths, setColumnWidths] = useState({});
  const windowDimensions = useContext(WindowDimensionsContext);

  useLayoutEffect(() => {
    if (tableRef.current) {
      // Calculate available width for columns which do not have a static width
      const assignableWidth = calculateAvailableWidth({
        actionsRef,
        columnRenderers,
        columnsIds,
        displayActionsCol,
        displayBulkSelectCol,
        tableWidth: tableRef.current.clientWidth,
      });
      setColumnWidths(columnsWidth({ assignableWidth, columnsIds, columnRenderers }));
    }
  }, [columnRenderers, columnsIds, displayActionsCol, displayBulkSelectCol, windowDimensions?.width]);

  return {
    tableRef,
    actionsRef,
    columnsWidths,
  };
};

export default useColumnWidths;
