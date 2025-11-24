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

import { DEFAULT_COL_MIN_WIDTH, DEFAULT_COL_WIDTH } from 'components/common/EntityDataTable/Constants';

import type { EntityBase, ColumnRenderersByAttribute } from '../types';

const assignableTableWidth = ({
  tableWidth,
  actionsColWidth,
  bulkSelectColWidth,
  columnIds,
  columnRenderersByAttribute,
  columnWidthPreferences,
}: {
  actionsColWidth: number;
  bulkSelectColWidth: number;
  columnRenderersByAttribute: { [columnId: string]: { staticWidth?: number } };
  columnIds: Array<string>;
  tableWidth: number;
  columnWidthPreferences: { [key: string]: number };
}) => {
  const staticColWidths = columnIds.reduce(
    (total, id) => total + (columnWidthPreferences[id] ?? columnRenderersByAttribute[id]?.staticWidth ?? 0),
    0,
  );

  return tableWidth - bulkSelectColWidth - actionsColWidth - staticColWidths;
};

const calculateColumnWidths = ({
  actionColMinWidth,
  assignableWidth,
  attributeColumnIds,
  attributeColumnRenderers,
  columnWidthPreferences,
}: {
  actionColMinWidth: number;
  assignableWidth: number;
  attributeColumnRenderers: { [columnId: string]: { staticWidth?: number; width?: number; minWidth?: number } };
  attributeColumnIds: Array<string>;
  columnWidthPreferences: { [key: string]: number };
}) => {
  const totalFlexColumns = attributeColumnIds.reduce((total, id) => {
    const { staticWidth, width = DEFAULT_COL_WIDTH } = attributeColumnRenderers[id] ?? {};

    if (columnWidthPreferences[id] ?? staticWidth) {
      return total;
    }

    return total + width;
  }, 0);

  const flexColWidth = assignableWidth / totalFlexColumns;

  return {
    ...Object.fromEntries(
      attributeColumnIds.map((id) => {
        const {
          staticWidth,
          width = DEFAULT_COL_WIDTH,
          minWidth = DEFAULT_COL_MIN_WIDTH,
        } = attributeColumnRenderers[id] ?? {};
        const targetWidth = columnWidthPreferences[id] ?? staticWidth ?? flexColWidth * width;

        return [id, !staticWidth && targetWidth < minWidth ? minWidth : targetWidth];
      }),
    ),
    actions: assignableWidth > 0 && !totalFlexColumns ? assignableWidth + actionColMinWidth : actionColMinWidth,
  };
};

const useColumnWidths = <Entity extends EntityBase>({
  actionsColWidth,
  bulkSelectColWidth,
  columnRenderersByAttribute,
  columnIds,
  tableWidth,
  columnWidthPreferences,
}: {
  actionsColWidth: number;
  bulkSelectColWidth: number;
  columnRenderersByAttribute: ColumnRenderersByAttribute<Entity>;
  columnIds: Array<string>;
  tableWidth: number;
  columnWidthPreferences: { [key: string]: number };
}) => {
  const [columnWidths, setColumnWidths] = useState({});

  useLayoutEffect(() => {
    if (!tableWidth) {
      return;
    }

    // Calculate available width for columns which do not have a static width
    const assignableWidth = assignableTableWidth({
      actionsColWidth,
      columnRenderersByAttribute,
      columnIds,
      bulkSelectColWidth,
      tableWidth,
      columnWidthPreferences,
    });

    // eslint-disable-next-line react-hooks/set-state-in-effect
    setColumnWidths(
      calculateColumnWidths({
        actionColMinWidth: actionsColWidth,
        assignableWidth,
        attributeColumnIds: columnIds,
        attributeColumnRenderers: columnRenderersByAttribute,
        columnWidthPreferences,
      }),
    );
  }, [actionsColWidth, bulkSelectColWidth, columnRenderersByAttribute, columnIds, tableWidth, columnWidthPreferences]);

  return columnWidths;
};

export default useColumnWidths;
