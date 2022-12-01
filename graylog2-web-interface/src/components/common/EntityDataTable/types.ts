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

export type Column = {
  anyPermissions?: boolean,
  id: string,
  permissions?: Array<string>
  sortable?: boolean,
  title: string,
  type?: boolean,
};

// A column render should have either a `width` and optionally a `minWidth` or only a `staticWidth`.
export type ColumnRenderer<Entity extends { id: string }> = {
  renderCell?: (entity: Entity, column: Column) => React.ReactNode,
  renderHeader?: (column: Column) => React.ReactNode,
  textAlign?: string,
  minWidth?: number, // px
  width?: number, // fraction of unassigned table width, similar to CSS unit fr.
  staticWidth?: number, // px
}

export type ColumnRenderers<Entity extends { id: string }> = {
  [columnId: string]: ColumnRenderer<Entity>
}

export type Sort = {
  columnId: string,
  order: 'asc' | 'desc'
};
