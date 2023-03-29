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

import type { Attributes, Attribute } from 'stores/PaginationTypes';

const dateAttribute: Attribute = {
  id: 'created_at',
  title: 'Created',
  type: 'DATE',
  sortable: true,
  filterable: true,
} as const;

const attributeWithRelactedCollection: Attribute = {
  id: 'index_set_id',
  title: 'Index set',
  type: 'STRING',
  sortable: true,
  filterable: true,
  hidden: true,
  related_collection: 'index_sets',
} as const;

const attributeWithFilterOptions: Attribute = {
  id: 'disabled',
  title: 'Status',
  type: 'BOOLEAN',
  sortable: true,
  filterable: true,
  filter_options: [
    {
      value: 'false',
      title: 'Running',
    },
    {
      value: 'true',
      title: 'Paused',
    },
  ],
};

// eslint-disable-next-line import/prefer-default-export
export const attributes: Attributes = [
  dateAttribute,
  attributeWithFilterOptions,
  attributeWithRelactedCollection,
];
