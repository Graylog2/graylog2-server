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

import type { Sort } from 'stores/PaginationTypes';

export const ENTITY_TABLE_ID = 'token_usage';
export const DEFAULT_LAYOUT = {
  entityTableId: 'token_usage',
  defaultPageSize: 20,
  defaultSort: { attributeId: 'NAME', direction: 'asc' } as Sort,
  defaultDisplayedAttributes: [
    'id',
    'username',
    'user_id',
    'NAME',
    'created_at',
    'expires_at',
    'last_access',
    'external_user',
    'title',
  ],
};
export const COLUMNS_ORDER = [
  'id',
  'username',
  'user_id',
  'NAME',
  'created_at',
  'last_access',    
  'expires_at',
  'external_user',
  'title',
];

export const ADDITIONAL_ATTRIBUTES = [];
