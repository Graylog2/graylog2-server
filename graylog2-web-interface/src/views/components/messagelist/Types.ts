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
// @flow strict
export type Message = {
  id: string,
  index: string,
  fields: { [key: string]: any },
  formatted_fields?: { [key: string]: any },
  highlight_ranges?: { [key: string]: any },
  decoration_stats?: {
    added_fields: { [key: string]: any },
    changed_fields: { [key: string]: any },
    removed_fields: { [key: string]: any },
  },
};

export type BackendMessage = {
  index: string,
  message: { _id: string } & {
    [key: string]: unknown,
  },
  highlight_ranges?: { [key: string]: any },
  decoration_stats?: {
    added_fields: { [key: string]: any },
    changed_fields: { [key: string]: any },
    removed_fields: { [key: string]: any },
  },
};
