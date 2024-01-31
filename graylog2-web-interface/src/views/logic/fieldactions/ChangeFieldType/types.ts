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

import type { Attribute, PaginatedListJSON } from 'stores/PaginationTypes';
import type { IndexSetFieldType, IndexSetFieldTypeJson } from 'components/indices/IndexSetFieldTypes/types';

export type TypeHistoryItem = string;

export type FieldTypeUsage = {
  id: string,
  indexSetTitle: string,
  streamTitles: Array<string>,
  types: Array<TypeHistoryItem>
}

export type FieldTypes = { [name: string]: string};

export type ChangeFieldTypeBodyJson = {
  index_sets: Array<string>,
  field: string,
  type: string,
  rotate: boolean,
}

export type ChangeFieldTypeBody = {
  indexSetSelection: Array<string>,
  newFieldType: string,
  field: string,
  rotated: boolean,
}

export type FieldTypeUsageElement = {
   index_set_id: string,
   index_set_title: string,
   stream_titles: Array<string>,
   types: Array<string>
}

export type PaginatedFieldTypeUsagesResponse = PaginatedListJSON & {
  elements: Array<FieldTypeUsageElement>,
  attributes: Array<Attribute>,
};

export type OnSubmitCallbackProps = {
  indexSetSelection: Array<string>,
  newFieldType: string,
  rotated: boolean,
  field: string,
}

export type FieldTypePutResponseJson = Record<string, IndexSetFieldTypeJson>
export type FieldTypePutResponse = Record<string, IndexSetFieldType>
