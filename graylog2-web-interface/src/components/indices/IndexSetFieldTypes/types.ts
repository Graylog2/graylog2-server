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

export type FieldTypeOrigin = 'INDEX' | 'OVERRIDDEN_INDEX' | 'OVERRIDDEN_PROFILE' | 'PROFILE';
export type IndexSetFieldTypeJson = {
  field_name: string,
  type: string,
  origin: FieldTypeOrigin,
  is_reserved: boolean,
}
export type IndexSetFieldType = {
  id: string,
  fieldName: string,
  origin: FieldTypeOrigin,
  isReserved: boolean,
  type: string,
}

export type ExpandedSectionProps = {
  type: string,
  fieldName?: string,
}
export type ProfileWithMappingsByField = {
  name: string,
  description?: string,
  id: string,
  customFieldMappingsByField: Record<string, string>
}
export type RemoveFieldTypeBody = {
  indexSets: Array<string>,
  fields: Array<string>,
  rotated: boolean,
}
export type RemoveFieldTypeBodyJson = {
  index_sets: Array<string>,
  fields: Array<string>,
  rotate: boolean,
}

export type SetIndexSetFieldTypeProfileBodyJson = {
  index_sets: Array<string>,
  rotate: boolean,
  profile_id: string,
}

export type SetIndexSetFieldTypeProfileBody = { indexSetId: string, rotated: boolean, profileId: string}

export type RemoveProfileFromIndexSetBody = { indexSetId: string, rotated: boolean, }
export type RemoveProfileFromIndexSetBodyJson = {
  index_sets: Array<string>,
  rotate: boolean,
}
