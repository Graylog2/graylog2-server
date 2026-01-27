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
import { createColumnHelper, type ColumnDef } from '@tanstack/react-table';
import { useMemo } from 'react';

import type { EntityBase, ColumnRenderersByAttribute } from 'components/common/EntityDataTable/types';
import type { ColumnSchema } from 'components/common/EntityDataTable';
import useBulkSelectColumnDefinition from 'components/common/EntityDataTable/hooks/useBulkSelectColumnDefinition';
import { BULK_SELECT_COL_ID, ACTIONS_COL_ID } from 'components/common/EntityDataTable/Constants';
import useActionsColumnDefinition from 'components/common/EntityDataTable/hooks/useActionsColumnDefinition';
import useAttributeColumnDefinitions from 'components/common/EntityDataTable/hooks/useAttributeColumnDefinitions';

const useColumnDefinitions = <Entity extends EntityBase, Meta>({
  actionsColMinWidth,
  columnRenderersByAttribute,
  columnSchemas,
  columnWidths,
  displayBulkSelectCol,
  entityActions,
  entityAttributesAreCamelCase,
  hasRowActions,
  meta,
  onActionsWidthChange,
  onChangeSlicing,
  onHeaderSectionResize,
  parentBgColor,
  appSection,
}: {
  actionsColMinWidth: number;
  columnRenderersByAttribute: ColumnRenderersByAttribute<Entity, Meta>;
  columnSchemas: Array<ColumnSchema>;
  columnWidths: { [_attributeId: string]: number };
  displayBulkSelectCol: boolean;
  entityActions?: (entity: Entity) => React.ReactNode;
  entityAttributesAreCamelCase: boolean;
  hasRowActions: boolean;
  meta: Meta;
  onActionsWidthChange: (colId: string, width: number) => void;
  onChangeSlicing: (sliceCol: string | undefined, slice?: string) => void;
  onHeaderSectionResize: (colId: string, part: 'left' | 'right', width: number) => void;
  parentBgColor: string | undefined;
  appSection?: string;
}) => {
  const columnHelper = createColumnHelper<Entity>();
  const bulkSelectCol = useBulkSelectColumnDefinition(displayBulkSelectCol, columnWidths[BULK_SELECT_COL_ID]);
  const actionsCol = useActionsColumnDefinition<Entity>({
    colWidth: columnWidths[ACTIONS_COL_ID],
    minWidth: actionsColMinWidth,
    entityActions,
    hasRowActions,
    onWidthChange: onActionsWidthChange,
    parentBgColor,
  });
  const attributeCols = useAttributeColumnDefinitions<Entity, Meta>({
    columnHelper,
    columnRenderersByAttribute,
    columnSchemas,
    columnWidths,
    entityAttributesAreCamelCase,
    meta,
    onChangeSlicing: onChangeSlicing,
    onHeaderSectionResize,
    appSection,
  });

  return useMemo(
    () =>
      [...(bulkSelectCol ? [bulkSelectCol] : []), ...attributeCols, ...(actionsCol ? [actionsCol] : [])] as Array<
        ColumnDef<Entity, unknown>
      >,
    [bulkSelectCol, attributeCols, actionsCol],
  );
};

export default useColumnDefinitions;
