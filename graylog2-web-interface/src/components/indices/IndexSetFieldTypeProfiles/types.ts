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

export type CustomFieldMapping = {
  field: string,
  type: string,
}
export type IndexSetFieldTypeProfileJson = {
  id: string,
  name: string,
  description: string,
  custom_field_mappings: Array<CustomFieldMapping>,
  index_set_ids?: Array<string>
}

export type IndexSetFieldTypeProfile = {
  id: string,
  name: string,
  description: string,
  customFieldMappings: Array<CustomFieldMapping>,
  indexSetIds: Array<string>
}

export type IndexSetFieldTypeProfileForm = {
  name: string,
  description: string,
  customFieldMappings: Array<CustomFieldMapping>,
}

export type IndexSetFieldTypeProfileRequest = {
  id?: string,
  name: string,
  description: string,
  customFieldMappings: Array<CustomFieldMapping>,
}

export type IndexSetFieldTypeProfileRequestJson = {
  id?: string,
  name: string,
  description: string,
  custom_field_mappings: Array<CustomFieldMapping>,
}

export type ProfileOptions = Array<{ value: string, label: string }>;
