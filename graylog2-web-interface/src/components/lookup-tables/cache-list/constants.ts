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

export const CACHE_TABLE_ID = 'cacheTableID';

export const attributes: Array<Attribute> = [
  {
    id: 'title',
    title: 'Title',
    type: 'STRING',
    searchable: true,
  },
  {
    id: 'description',
    title: 'Description',
    type: 'STRING',
  },
  {
    id: 'name',
    title: 'Name',
    type: 'STRING',
  },
  {
    id: 'entries',
    title: 'Entries',
    type: 'STRING',
  },
  {
    id: 'hit_rate',
    title: 'Hit Rate',
    type: 'STRING',
  },
  {
    id: 'throughput',
    title: 'Throughput',
    type: 'STRING',
  },
];

export const cacheListElements = {
  defaultLayout: {
    entityTableId: CACHE_TABLE_ID,
    defaultPageSize: 20,
    defaultSort: { attributeId: 'title', direction: 'asc' } as Sort,
    defaultDisplayedAttributes: ['title', 'description', 'name', 'entries', 'hit_rate', 'throughput'],
  },
  columnOrder: ['title', 'description', 'name', 'entries', 'hit_rate', 'throughput'],
};
