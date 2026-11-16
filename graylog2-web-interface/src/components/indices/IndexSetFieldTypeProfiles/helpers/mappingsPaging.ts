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
import type { FormikErrors } from 'formik';

import { defaultCompare } from 'logic/DefaultCompare';
import type {
  CustomFieldMapping,
  IndexSetFieldTypeProfileForm,
} from 'components/indices/IndexSetFieldTypeProfiles/types';

export const MAPPINGS_PAGE_SIZE = 50;

export const getLastPage = (rowsCount: number) => Math.max(1, Math.ceil(rowsCount / MAPPINGS_PAGE_SIZE));

export const getPageOfRow = (rowIndex: number) => Math.floor(rowIndex / MAPPINGS_PAGE_SIZE) + 1;

export const sortMappingsByField = (mappings: Array<CustomFieldMapping>) =>
  [...mappings].sort((a, b) => defaultCompare(a.field ?? '', b.field ?? ''));

export const getRowsMatchingSearch = (mappings: Array<CustomFieldMapping>, search: string) => {
  const query = search.trim().toLowerCase();

  return mappings
    .map((mapping, index) => ({ mapping, index }))
    .filter(({ mapping }) => (mapping.field ?? '').toLowerCase().includes(query));
};

export const getRowIndexesWithErrors = (errors: FormikErrors<IndexSetFieldTypeProfileForm>['customFieldMappings']) =>
  Array.isArray(errors) ? errors.flatMap((rowErrors, index) => (rowErrors ? [index] : [])) : [];
