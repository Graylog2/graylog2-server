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

import type { Sort, Attribute } from 'stores/PaginationTypes';

export const DEFAULT_LAYOUT = {
  entityTableId: 'streamOutputFilter',
  defaultPageSize: 20,
  defaultSort: { attributeId: 'title', direction: 'asc' } as Sort,
  defaultDisplayedAttributes: ['title', 'description', 'status'],
};

export const COLUMNS_ORDER = ['title', 'description', 'status'];

export const ADDITIONAL_ATTRIBUTES: Array<Attribute> = [
  { id: 'title', title: 'Rule' },
  { id: 'description', title: 'Description' },
  { id: 'status', title: 'Status' },
];
