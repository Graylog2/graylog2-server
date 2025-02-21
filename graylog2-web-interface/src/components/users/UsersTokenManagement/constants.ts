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

import type {Sort} from 'stores/PaginationTypes';

export const ENTITY_TABLE_ID = 'token_usage';
export const DEFAULT_LAYOUT = {
  entityTableId: 'token_usage',
  defaultPageSize: 20,
  defaultSort: { attributeId: 'token_name', direction: 'asc' } as Sort,
  defaultDisplayedAttributes: ["token_id", "username", "user_id", "token_name", "created_at", "last_access", "user_is_external", "auth_backend"],
};
export const COLUMNS_ORDER = ["token_id", "username", "user_id", "token_name", "created_at", "last_access", "user_is_external", "auth_backend"];

export const ADDITIONAL_ATTRIBUTES = [
  { id: 'user_is_external', title: 'External User', hidden: false },
  { id: 'token_id', title: 'Token ID', hidden: false },
  { id: 'username', title: 'Username', hidden: false },
  { id: 'token_name', title: 'Name', hidden: false },
  { id: 'created_at', title: 'Created', hidden: false },
  { id: 'last_access', title: 'Last Accessed', hidden: false },
  { id: 'auth_backend', title: 'Authentication Backend', hidden: false },
];
