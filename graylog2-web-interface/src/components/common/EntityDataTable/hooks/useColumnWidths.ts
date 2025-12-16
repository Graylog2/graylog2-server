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
import { useState, useLayoutEffect, useMemo } from 'react';

import {
  DEFAULT_COL_MIN_WIDTH,
  DEFAULT_COL_WIDTH,
  ACTIONS_COL_ID,
  BULK_SELECT_COL_ID,
} from 'components/common/EntityDataTable/Constants';

import type { EntityBase, ColumnRenderersByAttribute } from '../types';

const assignableTableWidth = ({
  scrollContainerWidth,
  actionsColMinWidth,
  bulkSelectColWidth,
  columnIds,
  staticColumnWidths,
}: {
  actionsColMinWidth: number;
  bulkSelectColWidth: number;
  columnIds: Array<string>;
  scrollContainerWidth: number;
  staticColumnWidths: { [colId: string]: number };
}) => {
  const staticColWidths = columnIds.reduce((total, id) => total + (staticColumnWidths[id] ?? 0), 0);

  return scrollContainerWidth - bulkSelectColWidth - actionsColMinWidth - staticColWidths;
};

const calculateColumnWidths = ({
  actionsColMinWidth,
  assignableWidth,
  attributeColumnIds,
  attributeColumnRenderers,
  bulkSelectColWidth,
  staticColumnWidths,
  headerMinWidths,
}: {
  actionsColMinWidth: number;
  assignableWidth: number;
  attributeColumnRenderers: ColumnRenderersByAttribute<EntityBase>;
  attributeColumnIds: Array<string>;
  bulkSelectColWidth?: number;
  staticColumnWidths: { [colId: string]: number };
  headerMinWidths: { [colId: string]: number };
}) => {
  const totalFlexColumns = attributeColumnIds.reduce((total, id) => {
    const { width = DEFAULT_COL_WIDTH } = attributeColumnRenderers[id] ?? {};

    if (staticColumnWidths[id]) {
      return total;
    }

    return total + width;
  }, 0);

  const flexColWidth = assignableWidth / totalFlexColumns;

  return {
    ...Object.fromEntries(
      attributeColumnIds.map((id) => {
        const { width = DEFAULT_COL_WIDTH, minWidth } = attributeColumnRenderers[id] ?? {};
        const targetWidth = staticColumnWidths[id] ?? Math.floor(flexColWidth * width);
        const baseMinWidth = minWidth ?? DEFAULT_COL_MIN_WIDTH;
        const resolvedMinWidth = Math.max(baseMinWidth, headerMinWidths[id] ?? 0);

        return [id, !staticColumnWidths[id] && targetWidth < resolvedMinWidth ? resolvedMinWidth : targetWidth];
      }),
    ),
    [ACTIONS_COL_ID]:
      assignableWidth > 0 && !totalFlexColumns ? assignableWidth + actionsColMinWidth : actionsColMinWidth,
    ...(bulkSelectColWidth ? { [BULK_SELECT_COL_ID]: bulkSelectColWidth } : {}),
  };
};

const calculateStaticColumnWidths = ({
  attributeColumnIds,
  attributeColumnRenderers,
  columnWidthPreferences,
  headerMinWidths,
}: {
  attributeColumnIds: Array<string>;
  attributeColumnRenderers: ColumnRenderersByAttribute<EntityBase>;
  headerMinWidths: { [colId: string]: number };
  columnWidthPreferences: { [colId: string]: number } | undefined;
}) =>
  attributeColumnIds.reduce((staticWidths, id) => {
    const staticWidth = columnWidthPreferences?.[id] ?? attributeColumnRenderers[id]?.staticWidth;

    if (!staticWidth) {
      return staticWidths;
    }

    const resolvedStaticWidth =
      staticWidth === 'matchHeader' || staticWidth < headerMinWidths[id] ? headerMinWidths[id] : staticWidth;

    return resolvedStaticWidth ? { ...staticWidths, [id]: resolvedStaticWidth } : staticWidths;
  }, {});

const useColumnWidths = <Entity extends EntityBase>({
  actionsColMinWidth,
  bulkSelectColWidth,
  columnRenderersByAttribute,
  columnIds,
  scrollContainerWidth,
  columnWidthPreferences,
  headerMinWidths,
}: {
  actionsColMinWidth: number;
  bulkSelectColWidth: number;
  columnRenderersByAttribute: ColumnRenderersByAttribute<Entity>;
  columnIds: Array<string>;
  scrollContainerWidth: number;
  columnWidthPreferences: { [key: string]: number } | undefined;
  headerMinWidths: { [colId: string]: number };
}) => {
  const [columnWidths, setColumnWidths] = useState({});
  const staticColumnWidths = useMemo(
    () =>
      calculateStaticColumnWidths({
        attributeColumnIds: columnIds,
        attributeColumnRenderers: columnRenderersByAttribute,
        columnWidthPreferences,
        headerMinWidths,
      }),
    [columnIds, columnRenderersByAttribute, columnWidthPreferences, headerMinWidths],
  );

  useLayoutEffect(() => {
    if (!scrollContainerWidth) {
      return;
    }

    // Calculate available width for columns which do not have a static width
    const assignableWidth = assignableTableWidth({
      actionsColMinWidth,
      columnIds,
      bulkSelectColWidth,
      scrollContainerWidth,
      staticColumnWidths,
    });

    // eslint-disable-next-line react-hooks/set-state-in-effect
    setColumnWidths(
      calculateColumnWidths({
        actionsColMinWidth,
        assignableWidth,
        attributeColumnIds: columnIds,
        attributeColumnRenderers: columnRenderersByAttribute,
        bulkSelectColWidth,
        staticColumnWidths,
        headerMinWidths,
      }),
    );
  }, [
    actionsColMinWidth,
    bulkSelectColWidth,
    columnRenderersByAttribute,
    columnIds,
    scrollContainerWidth,
    columnWidthPreferences,
    staticColumnWidths,
    headerMinWidths,
  ]);

  return columnWidths;
};

export default useColumnWidths;
