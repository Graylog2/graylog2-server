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

export type TypeHistoryItem = string;

export type FieldTypeUsage = {
  id: string,
  indexSet: string,
  streams: Array<string>,
  typeHistory: Array<TypeHistoryItem>
}

export type FieldTypeOption = { id: string, label: string };

export type FieldTypeOptions = Array<FieldTypeOption>;

export type ChangeFieldTypeFormValues = { indexSetSelection: Array<string>, newFieldType: string, rotated: boolean }

export type ChangeFieldTypeBodyJson = {
  index_sets_ids: Array<string>,
  field_name: string,
  new_type: string,
  rotate_immediately: boolean,
}

export type ChangeFieldTypeBody = {
  indexSetSelection: Array<string>,
  newFieldType: string,
  field: string,
  rotated: boolean,
}
