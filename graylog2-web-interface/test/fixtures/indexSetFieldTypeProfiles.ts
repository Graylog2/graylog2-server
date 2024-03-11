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
import omit from 'lodash/omit';

import type { Attribute, Attributes } from 'stores/PaginationTypes';

export const profile1JSON = {
  custom_field_mappings: [{ field: 'http_method', type: 'string' }, { field: 'user_ip', type: 'ip' }],
  name: 'Profile 1',
  index_set_ids: [],
  description: 'Description 1',
  id: '111',
};
export const profile2JSON = {
  custom_field_mappings: [{ field: 'user_name', type: 'string' }, { field: 'logged_in', type: 'bool' }, { field: 'sum', type: 'int' }],
  name: 'Profile 2',
  description: 'Description 2',
  index_set_ids: [],
  id: '222',
};

export const profile1 = {
  customFieldMappings: [{ field: 'http_method', type: 'string' }, { field: 'user_ip', type: 'ip' }],
  name: 'Profile 1',
  description: 'Description 1',
  indexSetIds: [],
  id: '111',
};
export const profile2 = {
  customFieldMappings: [
    { field: 'user_name', type: 'string' },
    { field: 'logged_in', type: 'bool' },
    { field: 'sum', type: 'int' },
  ],
  name: 'Profile 2',
  indexSetIds: [],
  description: 'Description 2',
  id: '222',
};

export const indexSetsAttribute: Attribute = {
  id: 'index_set_ids',
  searchable: false,
  sortable: false,
  title: 'Used in',
  type: 'STRING',
};
export const attributes: Attributes = [
  {
    id: 'id',
    title: 'Profile Id',
    type: 'STRING',
    sortable: true,
    hidden: true,
  },
  {
    id: 'name',
    title: 'Profile Name',
    type: 'STRING',
    sortable: true,
    filterable: true,
    searchable: true,
  },
  {
    id: 'description',
    title: 'Profile Description',
    type: 'STRING',
    sortable: false,
    filterable: true,
    searchable: true,
  },
  {
    id: 'custom_field_mappings',
    title: 'Custom Field Mappings',
    type: 'STRING',
    sortable: false,
  },
  indexSetsAttribute,
];

export const formValuesProfile1 = omit(profile1, ['indexSetIds', 'id']);
export const requestBodyProfile1JSON = omit(profile1JSON, 'index_set_ids');
