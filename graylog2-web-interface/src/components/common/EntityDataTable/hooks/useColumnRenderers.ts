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
import merge from 'lodash/merge';

import type { EntityBase } from 'components/common/EntityDataTable/types';
import type { ColumnSchema, ColumnRenderers } from 'components/common/EntityDataTable';
import DefaultColumnRenderers from 'components/common/EntityDataTable/DefaultColumnRenderers';

const useColumnRenderers = <Entity extends EntityBase, Meta = unknown>(
  columnSchemas: Array<ColumnSchema>,
  customColumnRenderers: ColumnRenderers<Entity, Meta>,
) =>
  useMemo(() => {
    const renderers = merge({}, DefaultColumnRenderers, customColumnRenderers);

    return Object.fromEntries(
      columnSchemas.map(({ id, type }) => {
        const typeRenderer = renderers.types?.[type];
        const attributeRenderer = renderers.attributes?.[id];

        const columnRenderer = merge({}, typeRenderer, attributeRenderer);

        return [id, columnRenderer];
      }),
    );
  }, [columnSchemas, customColumnRenderers]);

export default useColumnRenderers;
