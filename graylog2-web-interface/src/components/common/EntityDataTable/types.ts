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

export type Attribute = {
  id: string,
  title: string,
  sortable?: boolean,
  type?: boolean,
  permissions?: Array<string>
  anyPermissions?: boolean,
};

export type CustomHeaders = {
  [key: string]: {
    renderHeader: (attribute: Attribute) => React.ReactNode,
    textAlign?: string,
  }
}

export type CustomCells<ListItem extends { id: string }> = {
  [attributeId: string]: {
    renderCell: (listItem: ListItem, attribute: Attribute) => React.ReactNode,
    textAlign?: string,
    width?: string,
    maxWidth?: string,
  }
}

export type Sort = {
  attributeId: string,
  order: 'asc' | 'desc'
};
