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

import type { Column, ColumnRenderers } from 'components/common/EntityDataTable';
import { BULK_SELECT_COLUMN_WIDTH } from 'components/common/EntityDataTable/TableRow';
import WindowDimensionsContext from 'contexts/WindowDimensionsContext';

const calculateAvailableWidth = ({
  tableWidth,
  displayBulkActionsCol,
  displayActionsCol,
  actionsRef,
  columns,
  columnRenderers,
}: {
  actionsRef: React.RefObject<HTMLDivElement>,
  columnRenderers: { [columnId: string]: { width?: number } }
  columns: Array<{ id: string }>,
  displayActionsCol: boolean,
  displayBulkActionsCol: boolean,
  tableWidth: number,
}) => {
  let availableWidth = tableWidth;

  if (displayBulkActionsCol) {
    availableWidth -= BULK_SELECT_COLUMN_WIDTH;
  }

  if (displayActionsCol && actionsRef.current) {
    availableWidth -= actionsRef.current.offsetWidth + 20;
  }

  columns.forEach((column) => {
    if (columnRenderers[column.id] && 'width' in columnRenderers[column.id]) {
      availableWidth -= columnRenderers[column.id].width;
    }
  });

  return availableWidth;
};

const calculateColumnsWidth = ({
  availableWidth,
  columns,
  columnRenderers,
}: {
  columnRenderers: { [columnId: string]: { width?: number, flexWidth?: number } }
  columns: Array<{ id: string }>,
  availableWidth: number,
}) => {
  const flexTotal = columns.reduce((total, { id }) => {
    const flexWidth = columnRenderers[id]?.width ? 0 : (columnRenderers[id]?.flexWidth ?? 1);

    return total + flexWidth;
  }, 0);

  const defaultColWidth = availableWidth / flexTotal;

  return columns.reduce((widths, { id }) => {
    const width = columnRenderers[id]?.width ?? (defaultColWidth * (columnRenderers[id].flexWidth ?? 1));

    return { ...widths, [id]: width };
  }, []);
};

const useCalculateColumnWidths = <Entity extends { id: string }>(columns: Array<Column>, columnRenderers: ColumnRenderers<Entity>, displayActionsCol: boolean, displayBulkActionsCol: boolean) => {
  const tableRef = useRef<HTMLTableElement>();
  const actionsRef = useRef<HTMLDivElement>();
  const [columnsWidths, setColumnWidths] = useState({});
  const windowDimensions = useContext(WindowDimensionsContext);

  useLayoutEffect(() => {
    if (tableRef.current) {
      // Calculate available width for columns which do not have a fixed width
      const availableWidth = calculateAvailableWidth({
        actionsRef,
        columnRenderers,
        columns,
        displayActionsCol,
        displayBulkActionsCol,
        tableWidth: tableRef.current.offsetWidth,
      });

      setColumnWidths(calculateColumnsWidth({ availableWidth, columns, columnRenderers }));
    }
  }, [columnRenderers, columns, displayActionsCol, displayBulkActionsCol, windowDimensions?.width]);

  return {
    tableRef,
    actionsRef,
    columnsWidths: columnsWidths,
    actionsColWidth: (actionsRef.current?.offsetWidth ?? 0) + 20,
  };
};

export default useCalculateColumnWidths;
