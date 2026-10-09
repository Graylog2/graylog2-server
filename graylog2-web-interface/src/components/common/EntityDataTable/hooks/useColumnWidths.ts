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
import { TABLE_BORDER_WIDTH } from 'components/bootstrap/Table';
import { DEFAULT_COL_WIDTH, ACTIONS_COL_ID, BULK_SELECT_COL_ID } from 'components/common/EntityDataTable/Constants';

import type { EntityBase, ColumnRenderer, ColumnRenderersByAttribute } from '../types';

const resolveColumnWidth = ({
  columnRenderer,
  columnWidthPreference,
  headerMinWidth,
}: {
  columnRenderer: ColumnRenderer<EntityBase> | undefined;
  columnWidthPreference: number | undefined;
  headerMinWidth: number | undefined;
}) => {
  const width = columnWidthPreference ?? columnRenderer?.staticWidth ?? DEFAULT_COL_WIDTH;

  if (width === 'matchHeader') {
    // The header width is only known after the first render.
    return headerMinWidth ?? DEFAULT_COL_WIDTH;
  }

  return Math.max(width, headerMinWidth ?? 0);
};

const calculateColumnWidths = ({
  actionsColMinWidth,
  attributeColumnIds,
  attributeColumnRenderers,
  bulkSelectColWidth,
  columnWidthPreferences,
  headerMinWidths,
  scrollContainerWidth,
}: {
  actionsColMinWidth: number;
  attributeColumnIds: Array<string>;
  attributeColumnRenderers: ColumnRenderersByAttribute<EntityBase>;
  bulkSelectColWidth: number;
  columnWidthPreferences: { [colId: string]: number } | undefined;
  headerMinWidths: { [colId: string]: number };
  scrollContainerWidth: number;
}) => {
  const attributeColumnWidths = Object.fromEntries(
    attributeColumnIds.map((id) => [
      id,
      resolveColumnWidth({
        columnRenderer: attributeColumnRenderers[id],
        columnWidthPreference: columnWidthPreferences?.[id],
        headerMinWidth: headerMinWidths[id],
      }),
    ]),
  );
  const attributeColumnsWidth = Object.values(attributeColumnWidths).reduce((total, width) => total + width, 0);
  // The table's own outer border (left edge) is real box-model width under border-collapse: separate,
  // so it must be reserved here or the columns overflow the scroll container.
  const remainingWidth = scrollContainerWidth - TABLE_BORDER_WIDTH - bulkSelectColWidth - attributeColumnsWidth;

  return {
    ...attributeColumnWidths,
    // The actions column is the elastic tail. It fills the remaining space, so the table spans the container.
    [ACTIONS_COL_ID]: Math.max(actionsColMinWidth, remainingWidth),
    ...(bulkSelectColWidth ? { [BULK_SELECT_COL_ID]: bulkSelectColWidth } : {}),
  };
};

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
}): { [colId: string]: number } => {
  if (!scrollContainerWidth) {
    return {};
  }

  return calculateColumnWidths({
    actionsColMinWidth,
    attributeColumnIds: columnIds,
    attributeColumnRenderers: columnRenderersByAttribute,
    bulkSelectColWidth,
    columnWidthPreferences,
    headerMinWidths,
    scrollContainerWidth,
  });
};

export default useColumnWidths;
