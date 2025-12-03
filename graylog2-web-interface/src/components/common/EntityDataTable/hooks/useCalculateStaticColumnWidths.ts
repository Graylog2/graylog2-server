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

import { useMemo } from 'react';
import type { DefaultTheme } from 'styled-components';
import { useTheme } from 'styled-components';

import { SORT_ICON_WIDTH } from 'components/common/EntityDataTable/SortIcon';
import type { ColumnRenderersByAttribute, EntityBase } from 'components/common/EntityDataTable/types';
import type { ColumnSchema } from 'components/common/EntityDataTable';
import { DRAG_HANDLE_WIDTH } from 'components/common/SortableList/DragHandle';
import useMeasureTextWidth from 'components/common/EntityDataTable/hooks/useMeasureTextWidth';

const HEADER_PADDING = 10; // px
const DEFAULT_FONT_WEIGHT = 700;

const calculateHeaderWidth = (
  theme: DefaultTheme,
  columnId: string,
  measureTextWidth: (text: string) => number,
  columnSchemas: Array<ColumnSchema>,
) => {
  const columnSchema = columnSchemas.find(({ id }) => id === columnId);
  const sortIconWidthTotal = columnSchema?.sortable ? SORT_ICON_WIDTH + theme.spacings.px.sm : 0;
  const dragHandleWithTotal = DRAG_HANDLE_WIDTH + theme.spacings.px.xxs;

  return measureTextWidth(columnSchema?.title) + HEADER_PADDING + dragHandleWithTotal + sortIconWidthTotal;
};

const useCalculateStaticColumnWidths = ({
  attributeColumnIds,
  attributeColumnRenderers,
  columnSchemas,
}: {
  attributeColumnIds: Array<string>;
  attributeColumnRenderers: ColumnRenderersByAttribute<EntityBase>;
  columnSchemas: Array<ColumnSchema>;
}) => {
  const theme = useTheme();
  const measureTextWidth = useMeasureTextWidth({ weight: DEFAULT_FONT_WEIGHT });

  return useMemo(
    () =>
      attributeColumnIds.reduce<Record<string, number>>((staticWidths, id) => {
        const staticWidth = attributeColumnRenderers[id]?.staticWidth;

        if (!staticWidth) {
          return staticWidths;
        }

        const resolvedStaticWidth =
          staticWidth === 'matchHeader'
            ? calculateHeaderWidth(theme, id, measureTextWidth, columnSchemas)
            : staticWidth;

        return resolvedStaticWidth ? { ...staticWidths, [id]: resolvedStaticWidth } : staticWidths;
      }, {}),
    [attributeColumnIds, attributeColumnRenderers, columnSchemas, measureTextWidth, theme],
  );
};

export default useCalculateStaticColumnWidths;
