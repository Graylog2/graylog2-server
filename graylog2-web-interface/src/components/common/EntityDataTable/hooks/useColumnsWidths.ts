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
import { useState, useLayoutEffect } from 'react';

import type { ColumnRenderers } from 'components/common/EntityDataTable';
import {
  DEFAULT_COL_MIN_WIDTH,
  DEFAULT_COL_WIDTH,
} from 'components/common/EntityDataTable/Constants';

const assignableTableWidth = ({
  tableWidth,
  actionsColWidth,
  bulkSelectColWidth,
  columnsIds,
  columnRenderers,
}: {
  actionsColWidth: number,
  bulkSelectColWidth: number,
  columnRenderers: { [columnId: string]: { staticWidth?: number } }
  columnsIds: Array<string>,
  tableWidth: number,
}) => {
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

const useColumnsWidths = <Entity extends { id: string }>({
  actionsColWidth,
  bulkSelectColWidth,
  columnRenderers,
  columnsIds,
  tableWidth,
}: {
  actionsColWidth: number,
  bulkSelectColWidth: number,
  columnRenderers: ColumnRenderers<Entity>,
  columnsIds: Array<string>,
  tableWidth: number,
},
) => {
  const [columnsWidths, setColumnWidths] = useState({});

  useLayoutEffect(() => {
    if (!tableWidth) {
      return;
    }

    // Calculate available width for columns which do not have a static width
    const assignableWidth = assignableTableWidth({
      actionsColWidth,
      columnRenderers,
      columnsIds,
      bulkSelectColWidth,
      tableWidth,
    });

<<<<<<< HEAD
    setColumnWidths(columnsWidth({ assignableWidth, columnsIds, columnRenderers }));
=======
    setColumnWidths(columnsWidth({
      assignableWidth,
      columnsIds,
      columnRenderers,
    }));
>>>>>>> dc4286653d (Adding test)
  }, [actionsColWidth, bulkSelectColWidth, columnRenderers, columnsIds, tableWidth]);

  return columnsWidths;
};

export default useColumnsWidths;
