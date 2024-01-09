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

export const customField = {
  id: 'field',
  fieldName: 'field',
  type: 'bool',
  isCustom: true,
  isReserved: false,
};

export const secondCustomField = {
  id: 'field-2',
  fieldName: 'field-2',
  type: 'bool',
  isCustom: true,
  isReserved: false,
};
export const reservedField = {
  id: 'field',
  fieldName: 'field',
  type: 'bool',
  isCustom: false,
  isReserved: true,
};

export const defaultField = {
  id: 'field',
  fieldName: 'field',
  type: 'bool',
  isCustom: false,
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
    id: 'is_custom',
    title: 'Custom',
    type: 'STRING',
    sortable: true,
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
