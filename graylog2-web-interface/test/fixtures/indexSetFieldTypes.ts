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

import type { Attributes } from 'stores/PaginationTypes';
import type { IndexSetFieldType } from 'components/indices/IndexSetFieldTypes/types';

export const overriddenIndexField: IndexSetFieldType = {
  id: 'field-1',
  fieldName: 'field-1',
  type: 'bool',
  origin: 'OVERRIDDEN_INDEX',
  isReserved: false,
};

export const overriddenProfileField: IndexSetFieldType = {
  id: 'field-2',
  fieldName: 'field-2',
  type: 'bool',
  origin: 'OVERRIDDEN_PROFILE',
  isReserved: false,
};

export const profileField: IndexSetFieldType = {
  id: 'field-3',
  fieldName: 'field-3',
  type: 'string',
  origin: 'PROFILE',
  isReserved: false,
};
export const reservedField: IndexSetFieldType = {
  id: 'field-4',
  fieldName: 'field-4',
  type: 'bool',
  origin: 'INDEX',
  isReserved: true,
};

export const defaultField: IndexSetFieldType = {
  id: 'field',
  fieldName: 'field',
  type: 'bool',
  origin: 'INDEX',
  isReserved: false,
};

export const attributes: Attributes = [
  {
    id: 'field_name',
    title: 'Field Name',
    type: 'STRING',
    sortable: true,
  },
  {
    id: 'origin',
    title: 'Origin',
    type: 'STRING',
    sortable: true,
    filterable: true,
    filter_options: [
      {
        value: 'INDEX',
        title: 'Index',
      },
      {
        value: 'OVERRIDDEN_INDEX',
        title: 'Overridden index',
      },
      {
        value: 'OVERRIDDEN_PROFILE',
        title: 'Overridden profile',
      },
      {
        value: 'PROFILE',
        title: 'Profile',
      },
    ],
  },
  {
    id: 'is_reserved',
    title: 'Reserved',
    type: 'STRING',
    sortable: true,
  },
  {
    id: 'type',
    title: 'Type',
    type: 'STRING',
    sortable: true,
  },
];
